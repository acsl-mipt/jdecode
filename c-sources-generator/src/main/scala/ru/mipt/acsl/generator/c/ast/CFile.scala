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
  override def generate(s: CGeneratorState): Unit = {
    s.append("#define ").append(name)
    value.map(s.append(" ").append)
  }
}

object CDefine {
  def apply(name: String) = new CDefine(name, None)
  def apply(name: String, value: String) = new CDefine(name, Some(value))
}

class CIfNDef(val name: String, val statements: Seq[CStmt]) extends CMacroStmt {
  override def generate(s: CGeneratorState): Unit = {
    s.append("#ifndef ").append(name)
    s.eol()
    statements.foreach(stmt => { stmt.generate(s); s.eol() })
  }
}

object CIfNDef {
  def apply(name: String, statements: Seq[CStmt]) = new CIfNDef(name, statements)
}

class CEndIf extends CMacroStmt {
  override def generate(s: CGeneratorState): Unit = {
    s.append("#endif")
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
  override def generate(s: CGeneratorState): Unit = {
    subType.generate(s)
    s.append("*")
  }
}

case class CTypeDefStmt(name: String, t: CType) extends HStmt {
  override def generate(s: CGeneratorState): Unit = {
    s.append("typedef ")
    t.generate(s)
    s.append(" ").append(name).append(";")
  }
}

class CNativeType(name: String) extends CType {
  override def generate(s: CGeneratorState): Unit = {
    s.append(name)
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
  override def generate(s: CGeneratorState): Unit = {
    s.append(name)
  }
}

class CEnumTypeDefConst(val name: String, val value: Int) {

}

object CEnumTypeDefConst {
  def apply(name: String, value: Int) = new CEnumTypeDefConst(name, value)
}

trait CTypeDef extends CType

class CEnumTypeDef(consts: Iterable[CEnumTypeDefConst], name: Option[String] = None) extends CTypeDef {
  override def generate(s: CGeneratorState): Unit = {
    s.append("enum")
    name.map(s.append(" ").append)
    s.append(" {")
    var first = true
    consts.map(c => { if (first) first = !first else s.append(","); s.append(" ").append(c.name).append(" = ").append(c.value.toString);  })
    s.append(" }")
  }
}

object CEnumTypeDef {
  def apply(consts: Iterable[CEnumTypeDefConst]) = new CEnumTypeDef(consts)
}

case class CStructTypeDefField(name: String, t: CType) {

}

case class CStructTypeDef(fields: Traversable[CStructTypeDefField], name: Option[String] = None) extends CType {
  override def generate(s: CGeneratorState): Unit = {
    s.append("struct ")
    name.map(s.append(_).append(" "))
    s.append("{")
    s.incIndentation()
    var first = true
    fields.foreach(f => {
      if (first) {
        first = !first
        s.eol()
      }
      s.indent()
      f.t.generate(s)
      s.append(" ").append(f.name).append(";")
      s.eol()
    })
    s.decIndentation()
    s.append("}")
  }
}

class AstFile[A <: CStmt](val statements: mutable.Buffer[A]) extends Growable[A] {
  override def +=(elem: A): AstFile.this.type = { statements += elem; this }
  override def clear(): Unit = statements.clear()
}

class CFile[A <: CStmt](statements: mutable.Buffer[A]) extends AstFile[A](statements) with Generatable[CGeneratorState] {

  override def generate(generatorState: CGeneratorState): Unit = {
    statements.foreach(stmt => { stmt.generate(generatorState); generatorState.eol() })
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
  private var indentation: Int = 0

  def indent() = append("  " * indentation)

  def decIndentation() = indentation -= 1

  def incIndentation() = indentation += 1

  def append(s: String): CGeneratorState = { a.append(s); this }

  def eol(): Unit = {
    a.append("\n")
  }
}

object CGeneratorState {
  def apply(a: Appendable) = new CGeneratorState(a)
}
