package ru.mipt.acsl.generator.cpp.ast

import ru.mipt.acsl.generation.Generatable

import scala.collection.generic.Growable
import scala.collection.mutable

/**
 * @author Artem Shein
 */

trait CppAstElement extends Generatable[CGeneratorState]

trait CppStmt extends CppAstElement
trait HStmt extends CppStmt

class HComment(val text: String) extends HStmt {
  override def generate(s: CGeneratorState): Unit = {
    s.append("/* ").append(text).append(" */")
  }
}

object HComment {
  def apply(text: String) = new HComment(text)
}

object HEol extends HStmt {
  override def generate(s: CGeneratorState): Unit = s.eol()
}

trait CppMacroStmt extends HStmt

class CppDefine(val name: String, val value: Option[String]) extends CppMacroStmt {
  override def generate(s: CGeneratorState): Unit = {
    s.append("#define ").append(name)
    value.map(s.append(" ").append)
    s.eol()
  }
}

object CppDefine {
  def apply(name: String) = new CppDefine(name, None)
  def apply(name: String, value: String) = new CppDefine(name, Some(value))
}

class CppIfNDef(val name: String, val statements: Seq[CppStmt]) extends CppMacroStmt {
  override def generate(s: CGeneratorState): Unit = {
    s.append("#ifndef ").append(name).eol()
    statements.foreach(stmt => { stmt.generate(s); s.eol() })
  }
}

object CppIfNDef {
  def apply(name: String, statements: Seq[CppStmt]) = new CppIfNDef(name, statements)
}

class CppEndIf extends CppMacroStmt {
  override def generate(s: CGeneratorState): Unit = {
    s.append("#endif")
  }
}

object CppEndIf {
  val obj = new CppEndIf()
  def apply() = obj
}

trait CppType extends CppAstElement {
  def ptr(): CppPtrType = CppPtrType(this)
}

case class CppPtrType(subType: CppType) extends CppType {
  override def generate(s: CGeneratorState): Unit = {
    subType.generate(s)
    s.append("*")
  }
}

case class CppTypeDefStmt(name: String, t: CppType) extends HStmt {
  override def generate(s: CGeneratorState): Unit = {
    s.append("typedef ")
    t.generate(s)
    s.append(" ").append(name).append(";").eol()
  }
}

class CppNativeType(name: String) extends CppTypeApplication(name) {
  override def generate(s: CGeneratorState): Unit = {
    s.append(name)
  }
}

case object CppVoidType$ extends CppNativeType("void")
case object CppUnsignedCharType$ extends CppNativeType("unsigned char")
case object CppSignedCharType$ extends CppNativeType("signed char")
case object CppUnsignedShortType$ extends CppNativeType("unsigned short")
case object CppSignedShortType$ extends CppNativeType("signed short")
case object CppUnsignedIntType$ extends CppNativeType("unsigned int")
case object CppSignedIntType$ extends CppNativeType("signed int")
case object CppUnsignedLongType$ extends CppNativeType("unsigned long")
case object CppSignedLongType$ extends CppNativeType("signed long")
case object CppFloatType$ extends CppNativeType("float")
case object CppDoubleType$ extends CppNativeType("double")

class CppTypeApplication(val name: String) extends CppType {
  override def generate(s: CGeneratorState): Unit = {
    s.append(name)
  }
}

object CppTypeApplication {
  def apply(name: String): CppTypeApplication = new CppTypeApplication(name)
}

class CEnumTypeDefConst(val name: String, val value: Int) {

}

object CEnumTypeDefConst {
  def apply(name: String, value: Int) = new CEnumTypeDefConst(name, value)
}

trait CppTypeDef extends CppType

class CppEnumTypeDef(consts: Iterable[CEnumTypeDefConst], name: Option[String] = None) extends CppTypeDef {
  override def generate(s: CGeneratorState): Unit = {
    s.append("enum")
    name.map(s.append(" ").append)
    s.append(" {")
    var first = true
    consts.map(c => { if (first) first = !first else s.append(","); s.append(" ").append(c.name).append(" = ").append(c.value.toString);  })
    s.append(" }")
  }
}

object CppEnumTypeDef {
  def apply(consts: Iterable[CEnumTypeDefConst]) = new CppEnumTypeDef(consts)
}

case class CStructTypeDefField(name: String, t: CppType) {

}

case class CppStructTypeDef(fields: Traversable[CStructTypeDefField], name: Option[String] = None) extends CppType {
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
      s.append(" ").append(f.name).append(";").eol()
    })
    s.decIndentation()
    s.append("}")
  }
}

class AstFile[A <: CppStmt](val statements: mutable.Buffer[A]) extends Growable[A] {
  override def +=(elem: A): AstFile.this.type = { statements += elem; this }
  override def clear(): Unit = statements.clear()
}

class CFile[A <: CppStmt](statements: mutable.Buffer[A]) extends AstFile[A](statements) with Generatable[CGeneratorState] {

  override def generate(s: CGeneratorState): Unit = {
    statements.foreach(_.generate(s))
  }

  override def +=(elem: A): CFile.this.type = {
    statements += elem
    this
  }

  override def clear(): Unit = {
    statements.clear()
  }
}

object CFile {
  def apply(statements: CppStmt*) = new CFile[CppStmt](statements.to[mutable.Buffer])
}

class HFile(statements: mutable.Buffer[HStmt]) extends CFile[HStmt](statements) {

}

object HFile {
  def apply() = new HFile(mutable.Buffer())
  def apply(statements: HStmt*) = new HFile(statements.to[mutable.Buffer])
}

class CGeneratorState(var a: Appendable) {
  private var indentation: Int = 0

  def indent() = append("  " * indentation)

  def decIndentation() = indentation -= 1

  def incIndentation() = indentation += 1

  def append(s: String): CGeneratorState = { a.append(s); this }

  def eol(): CGeneratorState = {
    a.append("\n")
    this
  }
}

object CGeneratorState {
  def apply(a: Appendable) = new CGeneratorState(a)
}
