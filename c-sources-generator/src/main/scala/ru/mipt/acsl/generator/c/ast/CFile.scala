package ru.mipt.acsl.generator.c.ast

import java.security.MessageDigest

import ru.mipt.acsl.generation.Generatable

import scala.collection.generic.Growable
import scala.collection.mutable
import scala.util.Random

/**
 * @author Artem Shein
 */

trait CAstElement extends Generatable[CGeneratorState]

trait CStmt extends CAstElement
trait HStmt extends CStmt

trait CMacroStmt extends HStmt

class CDefine(val name: String, val value: Option[String]) extends CMacroStmt {
  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    appendable.append("#define ").append(name)
    value.map(appendable.append(" ").append(_))
  }
}

object CDefine {
  def apply(name: String) = new CDefine(name, None)
  def apply(name: String, value: String) = new CDefine(name, Some(value))
}

class CIfNDef(val name: String, val statements: Seq[CStmt]) extends CMacroStmt {
  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    appendable.append("#ifndef ").append(name)
    generatorState.eol(appendable)
    statements.foreach(stmt => { stmt.generate(generatorState, appendable); generatorState.eol(appendable) })
  }
}

object CIfNDef {
  def apply(name: String, statements: Seq[CStmt]) = new CIfNDef(name, statements)
}

class CEndIf extends CMacroStmt {
  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    appendable.append("#endif")
  }
}

object CEndIf {
  val obj = new CEndIf()
  def apply() = obj
}

trait CType extends CAstElement {
  def ptr(): CPtrType = CPtrType(this)
}

case class CPtrType(subType: CType) extends CType {
  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    subType.generate(generatorState, appendable)
    appendable.append("*")
  }
}

case class CTypeDefStmt(name: String, t: CType) extends HStmt {
  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    appendable.append("typedef ")
    t.generate(generatorState, appendable)
    appendable.append(" ").append(name).append(";")
  }
}

class CNativeType(name: String) extends CType {
  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    appendable.append(name)
  }
}

case object CVoidType extends CNativeType("void")
case object CUnsignedCharType extends CNativeType("unsigned char")
case object CSignedCharType extends CNativeType("signed char")
case object CUnsignedShortType extends CNativeType("unsigned short")
case object CSignedShortType extends CNativeType("signed short")
case object CUnsignedIntType extends CNativeType("unsigned int")
case object CSignedIntType extends CNativeType("signed int")
case object CUnsignedLongType extends CNativeType("unsigned long")
case object CSignedLongType extends CNativeType("signed long")
case object CFloatType extends CNativeType("float")
case object CDoubleType extends CNativeType("double")

case class CTypeApplication(name: String) extends CType {
  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    appendable.append(name)
  }
}

class CEnumTypeDefConst(val name: String, val value: Int) {

}

object CEnumTypeDefConst {
  def apply(name: String, value: Int) = new CEnumTypeDefConst(name, value)
}

trait CTypeDef extends CType

class CEnumTypeDef(consts: Iterable[CEnumTypeDefConst], name: Option[String] = None) extends CTypeDef {
  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    appendable.append("enum")
    name.map(appendable.append(" ").append(_))
    appendable.append(" {")
    var first = true
    consts.map(c => { if (first) first = !first else appendable.append(","); appendable.append(" ").append(c.name).append(" = ").append(c.value.toString);  })
    appendable.append(" }")
  }
}

object CEnumTypeDef {
  def apply(consts: Iterable[CEnumTypeDefConst]) = new CEnumTypeDef(consts)
}

case class CStructTypeDefField(name: String, t: CType) {

}

case class CStructTypeDef(fields: Traversable[CStructTypeDefField], name: Option[String] = None) extends CType {
  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    appendable.append("struct ")
    name.map(appendable.append(_).append(" "))
    appendable.append("{")
    generatorState.incIndentation()
    var first = true
    fields.foreach(f => {
      if (first) {
        first = !first
        generatorState.eol(appendable)
      }
      generatorState.indent(appendable)
      f.t.generate(generatorState, appendable)
      appendable.append(" ").append(f.name).append(";")
      generatorState.eol(appendable)
    })
    generatorState.decIndentation()
    appendable.append("}")
  }
}

class AstFile[A <: CStmt](val statements: mutable.Buffer[A]) extends Growable[A] {
  override def +=(elem: A): AstFile.this.type = { statements += elem; this }
  override def clear(): Unit = statements.clear()
}

class CFile[A <: CStmt](statements: mutable.Buffer[A]) extends AstFile[A](statements) with Generatable[CGeneratorState] {

  override def generate(generatorState: CGeneratorState, appendable: Appendable): Unit = {
    statements.foreach(stmt => { stmt.generate(generatorState, appendable); generatorState.eol() })
  }

  override def +=(elem: A): CFile.this.type = {
    statements += elem
    this
  }

  override def clear(): Unit = {
    statements.clear()
  }
}

class HFile(statements: mutable.Buffer[HStmt]) extends CFile(statements) {

}

object HFile {
  def apply() = new HFile(mutable.Buffer())
  def apply(statements: mutable.Buffer[HStmt]) = new HFile(statements)
  def apply(statements: Seq[HStmt]) = new HFile(mutable.Buffer() ++ statements)
}

class CGeneratorState(var a: Appendable) {
  def eol(): Unit = {
    a.append("\n")
  }
}

object CGeneratorState {
  def apply() = new CGeneratorState()
}
