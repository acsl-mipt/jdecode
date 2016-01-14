package ru.mipt.acsl.generator.c.ast

import ru.mipt.acsl.generation.Generatable

import scala.collection.{immutable, mutable}

/**
  * @author Artem Shein
  */

trait CAstElement extends Generatable[CGeneratorState]

trait CStatement extends CAstElement

trait CExpression extends CAstElement

case class CExprStatement(expr: CExpression) extends CStatement {
  override def generate(s: CGeneratorState): Unit = {
    expr.generate(s)
    s.append(";")
  }
}

object CStatement {
  def apply(expr: CExpression) = CExprStatement(expr)
}

class CComment(val text: String) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = {
    s.append("/* ").append(text).append(" */")
  }
}

object CComment {
  def apply(text: String): CComment = new CComment(text)
}

object CEol extends CAstElement {
  override def generate(s: CGeneratorState): Unit = s.eol()
}

trait CMacroAstElement extends CAstElement

class CDefine(val name: String, val value: Option[String]) extends CMacroAstElement {
  override def generate(s: CGeneratorState): Unit = {
    s.append("#define ").append(name)
    value.foreach(s.append(" ").append)
    s.eol()
  }
}

object CDefine {
  def apply(name: String) = new CDefine(name, None)

  def apply(name: String, value: String) = new CDefine(name, Some(value))
}

class CPragma(val value: String) extends CMacroAstElement {
  override def generate(s: CGeneratorState): Unit = {
    s.append("#pragma ").append(value).eol()
  }
}

object CPragma {
  def apply(value: String) = new CPragma(value)
}

object CIfDef {
  type Type = CAstElements[CAstElement] with CMacroAstElement

  private class Impl(val name: String, elements: CAstElement*) extends CAstElements(elements.to[immutable.Seq])
  with CMacroAstElement {
    override def generate(s: CGeneratorState): Unit = {
      s.append(s"#ifdef $name").eol()
      Helpers.generate(s, elements)
    }
  }

  def apply(name: String, statements: CAstElement*): Type = new Impl(name, statements: _*)
}

object CIfNDef {
  type Type = CIfDef.Type

  private class Impl(val name: String, elements: immutable.Seq[CAstElement]) extends CAstElements(elements)
  with CMacroAstElement {
    override def generate(s: CGeneratorState): Unit = {
      s.append(s"#ifndef $name").eol()
      elements.foreach(stmt => {
        stmt.generate(s); s.eol()
      })
    }
  }

  def apply(name: String, statements: Seq[CAstElement]): Type = new Impl(name, statements.to[immutable.Seq])
}

class CEndIf extends CMacroAstElement {
  override def generate(s: CGeneratorState): Unit = s.append("#endif")
}

object CEndIf {
  val obj = new CEndIf()

  def apply() = obj
}

class CInclude(val path: String) extends CMacroAstElement {
  override def generate(s: CGeneratorState) {
    s.append("#include \"").append(path).append("\"")
  }
}

case class CPlainText(text: String) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = s.append(text)
}

object CInclude {
  def apply(path: String) = new CInclude(path)
}

trait CType extends CAstElement {
  def ptr: CPtrType = CPtrType(this)
}

case class CPtrType(subType: CType) extends CType {
  override def generate(s: CGeneratorState): Unit = {
    subType.generate(s)
    s.append("*")
  }
}

case class CTypeDefStatement(name: String, t: CType) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = {
    s.append("typedef ")
    t.generate(s)
    s.append(" ").append(name).append(";").eol()
  }
}

class CNativeType(name: String) extends CTypeApplication(name) {
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

class CTypeApplication(val name: String) extends CType {
  override def generate(s: CGeneratorState): Unit = s.append(name)
}

object CTypeApplication {
  def apply(name: String): CTypeApplication = new CTypeApplication(name)
}

class CEnumTypeDefConst(val name: String, val value: Int)

object CEnumTypeDefConst {
  def apply(name: String, value: Int) = new CEnumTypeDefConst(name, value)
}

trait CTypeDef extends CType

class CEnumTypeDef(consts: Iterable[CEnumTypeDefConst], name: Option[String] = None) extends CTypeDef {
  override def generate(s: CGeneratorState): Unit = {
    s.append("enum")
    name.foreach(s.append(" ").append)
    s.append(" {")
    var first = true
    consts.map(c => {
      if (first) first = !first else s.append(","); s.append(" ").append(c.name).append(" = ").append(c.value.toString);
    })
    s.append(" }")
  }
}

object CEnumTypeDef {
  def apply(consts: Iterable[CEnumTypeDefConst]) = new CEnumTypeDef(consts)
}

case class CStructTypeDefField(name: String, t: CType)

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
      s.append(" ").append(f.name).append(";").eol()
    })
    s.decIndentation()
    s.append("}")
  }
}

case class Parameter(name: String, t: CType) extends Generatable[CGeneratorState] {
  override def generate(s: CGeneratorState): Unit = {
    t.generate(s)
    s.append(s" $name")
  }
}

private object Helpers {

  implicit class Elements(val seq: Seq[CAstElement])

  def generate(s: CGeneratorState, elements: Elements): Unit = {
    elements.seq.foreach(stmt => {
      stmt.generate(s); s.eol()
    })
  }

  def generate(s: CGeneratorState, parameters: Parameter*): Unit = {
    s.append("(")
    var first = true
    parameters.foreach { p => if (first) first = false else s.append(", "); p.generate(s) }
    s.append(")")
  }

  def generate(s: CGeneratorState, statements: CStatements) = {
    s.append(" {").eol().incIndentation()
    statements.elements.foreach({ statement => s.indent(); statement.generate(s); s.eol() })
    s.decIndentation().indent().append("}")
  }
}

case class CCase(expr: CExpression, statements: CStatements) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = {
    s.indent()
    s.append("case ")
    expr.generate(s)
    s.append(":").incIndentation().eol()
    statements.generate(s)
    s.decIndentation()
  }
}

case class CSwitch(expr: CExpression, cases: Seq[CCase], default: Option[CStatements] = None) extends CStatement {
  override def generate(s: CGeneratorState): Unit = {
    s.append("switch (")
    expr.generate(s)
    s.append(") {").incIndentation().eol()
    cases.foreach({ c => c.generate(s); s.eol() })
    default.foreach({ statements => s.indent().append("default:").eol().incIndentation(); statements.generate(s); s.decIndentation().eol() })
    s.decIndentation().indent().append("}")
  }
}

case class CStringLiteral(value: String) extends CExpression {
  override def generate(s: CGeneratorState): Unit = s.append("\"").append(value.replace("\"", "\\\"")).append("\"")
}

case class CIntLiteral(value: Int) extends CExpression {
  override def generate(s: CGeneratorState): Unit = s.append(value.toString)
}

case class MutableCAstElements[A <: CAstElement](statements: mutable.Buffer[A] = mutable.Buffer.empty) extends CAstElement {
  def nonEmpty = statements.nonEmpty
  def isEmpty = statements.isEmpty
  def +=(el: A) = { statements += el }
  def ++=(elements: Seq[A]) = { statements ++= elements; this }
  override def generate(s: CGeneratorState): Unit = statements.foreach(_.generate(s))
}

case class CAstElements[+A <: CAstElement](elements: immutable.Seq[A] = immutable.Seq.empty)
  extends CAstElement {
  def nonEmpty = elements.nonEmpty
  def isEmpty = elements.isEmpty

  override def generate(s: CGeneratorState): Unit = elements.foreach(_.generate(s))
}

class CStatements(statements: CStatement*) extends CAstElements(statements.to[immutable.Seq]) {
  override def generate(s: CGeneratorState) = {
    var first = true
    statements.foreach({ stmt =>
      if (first) first = false else s.eol()
      s.indent()
      stmt.generate(s);
    })
  }
}

object CStatements {
  def apply(statements: CStatement*) = new CStatements(statements: _*)
}

case class FuncCall(methodName: String, arguments: CExpression*) extends CExpression {
  override def generate(s: CGeneratorState): Unit = {
    s.append(methodName).append("(")
    var first = true
    arguments.foreach{ arg => if (first) first = false else s.append(", "); arg.generate(s) }
    s.append(")")
  }
}

case class CVar(name: String) extends CExpression {
  override def generate(s: CGeneratorState): Unit = s.append(name)
}

case class MacroCall(name: String, arguments: CExpression*) extends CExpression {
  override def generate(s: CGeneratorState): Unit = {
    s.append(name).append("(")
    var first = true
    arguments.foreach{ arg => if (first) first = false else s.append(", "); arg.generate(s) }
    s.append(")")
  }
}

package object Aliases {
  type StatementBuffer[+A <: CAstElement] = CAstElements[A] with CAstElement
}

case class Return(expression: CExpression) extends CStatement {
  override def generate(s: CGeneratorState): Unit = {
    s.append("return ")
    expression.generate(s)
    s.append(";")
  }
}

case class CIf(expression: CExpression, thenStatements: CStatements = CStatements(),
               elseStatements: CStatements = CStatements()) extends CStatement {
  override def generate(s: CGeneratorState): Unit = {
    s.append("if (")
    expression.generate(s)
    s.append(")")
    Helpers.generate(s, thenStatements)
    if (elseStatements.nonEmpty) {
      s.append(" else")
      Helpers.generate(s, elseStatements)
    }
  }
}

case class CVarDef(name: String, t: CType, init: Option[CExpression]) extends CStatement {
  override def generate(s: CGeneratorState): Unit = {
    t.generate(s)
    s.append(" ").append(name)
    init.foreach{s.append(" = "); _.generate(s)}
    s.append(";")
  }
}

case class CFuncDef(name: String, returnType: CType = CVoidType, parameters: Seq[Parameter] = Seq.empty) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = {
    returnType.generate(s)
    s.append(" ").append(name)
    Helpers.generate(s, parameters: _*)
  }
}

case class CFuncImpl(definition: CFuncDef, implementation: CStatements) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = {
    definition.generate(s)
    Helpers.generate(s, implementation)
  }
}

object CFile {
  type Type = CAstElements[CAstElement]

  private class Impl(statements: immutable.Seq[CAstElement]) extends CAstElements[CAstElement](statements)

  def apply(statements: CAstElement*): Type = new Impl(statements.to[immutable.Seq])
}

class CGeneratorState(var a: Appendable) {
  var pos: Int = 0
  var line: Int = 0
  var linePos: Int = 0
  private var indentation: Int = 0

  def indent() = append("  " * indentation)

  def decIndentation() = { indentation -= 1; this }

  def incIndentation() = { indentation += 1; this }

  private def advance(s: String) {
    pos += s.length
    linePos += s.length
  }

  def append(s: String): CGeneratorState = {
    advance(s)
    a.append(s); this
  }

  def eol(): CGeneratorState = {
    a.append("\n")
    line += 1
    linePos = 0
    this
  }
}

object CGeneratorState {
  def apply(a: Appendable) = new CGeneratorState(a)
}
