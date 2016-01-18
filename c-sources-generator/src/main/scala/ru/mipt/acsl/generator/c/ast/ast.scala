package ru.mipt.acsl.generator.c.ast

import ru.mipt.acsl.generation.Generatable

import scala.collection.mutable

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

case class CIfDef(name: String) extends CMacroAstElement {
  override def generate(s: CGeneratorState): Unit = {
    s.append(s"#ifdef $name")
  }
}

case class CIfNDef(name: String) extends CMacroAstElement {
  override def generate(s: CGeneratorState): Unit = {
    s.append(s"#ifndef $name")
  }
}

case object CEndIf extends CMacroAstElement {
  override def generate(s: CGeneratorState): Unit = s.append("#endif")
}

case class CInclude(path: String) extends CMacroAstElement {
  override def generate(s: CGeneratorState) {
    s.append("#include \"").append(path).append("\"")
  }
}

case class CPlainText(text: String) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = s.append(text)
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
  def apply(name: String) = new CTypeApplication(name)
}

case class CEnumTypeDefConst(name: String, value: Int)

case class CFuncType(returnType: CType, parameterTypes: Iterable[CType], name: String = "") extends CType {
  override def generate(s: CGeneratorState): Unit = {
    returnType.generate(s)
    s.append(s" (*$name)")
    Helpers.generate(s, parameterTypes)
  }
}

trait CTypeDef extends CType

case class CEnumTypeDef(consts: Iterable[CEnumTypeDefConst], name: Option[String] = None) extends CTypeDef {
  override def generate(s: CGeneratorState): Unit = {
    s.append("enum")
    name.foreach(s.append(" ").append)
    s.append(" {")
    var first = true
    consts.map(c => {
      if (first) first = !first else s.append(","); s.append(" ").append(c.name).append(" = ").append(c.value.toString)
    })
    s.append(" }")
  }
}

object CEnumTypeDef {
  def apply(consts: CEnumTypeDefConst*) = new CEnumTypeDef(consts)
}

case class CStructType(name: String) extends CType {
  override def generate(s: CGeneratorState): Unit = s.append(s"struct $name")
}

case class CStructTypeDefField(name: String, t: CType) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = {
    t match {
      case f: CFuncType => f.generate(s)
      case _ => t.generate(s); s.append(s" $name")
    }
  }
}

case class CForwardStructDecl(name: String) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = s.append(s"struct $name;")
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
      f.generate(s)
      s.append(";").eol()
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
  def generate(s: CGeneratorState, name: String, t: CType): Unit = {
    t.generate(s)
    s.append(s" $name")
  }

  def generate(s: CGeneratorState, parameterTypes: Iterable[CType]): Unit = {
    s.append("(")
    var first = true
    parameterTypes.foreach { p => if (first) first = false else s.append(", "); p.generate(s) }
    s.append(")")
  }

  implicit class Elements(val seq: Seq[CAstElement])

  def generate(s: CGeneratorState, elements: Elements): Unit = {
    elements.seq.foreach(_.generate(s))
  }

  def generate(s: CGeneratorState, parameters: Parameter*): Unit = {
    s.append("(")
    var first = true
    parameters.foreach { p => if (first) first = false else s.append(", "); p.generate(s) }
    s.append(")")
  }

  def generate(s: CGeneratorState, statements: CStatements) = {
    s.append(" {").eol().incIndentation()
    statements.buf.foreach({ statement => s.indent(); statement.generate(s); s.eol() })
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

case class CAstElements(buf: mutable.Buffer[CAstElement] = mutable.Buffer.empty) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = { Helpers.generate(s, buf) }
}

case class CStatements(buf: mutable.Buffer[CStatement] = mutable.Buffer.empty) extends CAstElement {
  override def generate(s: CGeneratorState) = {
    var first = true
    buf.foreach({ stmt =>
      if (first) first = false else s.eol()
      s.indent()
      stmt.generate(s);
    })
  }
}

case class CArrow(expr: CExpression, expr2: CExpression) extends CExpression {
  override def generate(s: CGeneratorState): Unit = {
    expr.generate(s)
    s.append("->")
    expr2.generate(s)
  }
}

case class CDot(expr: CExpression, expr2: CExpression) extends CExpression {
  override def generate(s: CGeneratorState): Unit = {
    expr.generate(s)
    s.append(".")
    expr2.generate(s)
  }
}

package object Implicits {
  implicit def cAstElement2cAstElements(el: CAstElement): CAstElements = CAstElements(mutable.Buffer(el))
  implicit def iterable2cAstElements(els: Iterable[CAstElement]): CAstElements = CAstElements(els.to[mutable.Buffer])
  implicit def cAstElements2buffer(els: CAstElements): mutable.Buffer[CAstElement] = els.buf
  implicit def cStatement2cStatements(el: CStatement): CStatements = CStatements(mutable.Buffer(el))
  implicit def iterable2cStatements(els: Iterable[CStatement]): CStatements = CStatements(els.to[mutable.Buffer])
}

case class CFuncCall(methodName: String, arguments: CExpression*) extends CExpression {
  override def generate(s: CGeneratorState): Unit = {
    s.append(methodName).append("(")
    var first = true
    arguments.foreach{ arg => if (first) first = false else s.append(", "); arg.generate(s) }
    s.append(")")
  }
}

case class CDefVar(name: String, t: CType, init: Option[CExpression] = None) extends CStatement {
  override def generate(s: CGeneratorState): Unit = {
    Helpers.generate(s, name, t)
    if (init.isDefined) {
      s.append(" = ")
      init.get.generate(s)
    }
    s.append(";")
  }
}

case class CVar(name: String) extends CExpression {
  override def generate(s: CGeneratorState): Unit = s.append(name)
}

case class MacroCall(name: String, arguments: CExpression*) extends CExpression with CStatement {
  override def generate(s: CGeneratorState): Unit = {
    s.append(name).append("(")
    var first = true
    arguments.foreach{ arg => if (first) first = false else s.append(", "); arg.generate(s) }
    s.append(")")
  }
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
    if (elseStatements.buf.nonEmpty) {
      s.append(" else")
      Helpers.generate(s, elseStatements)
    }
  }
}

case object CIdent extends CAstElement {
  override def generate(s: CGeneratorState): Unit = s.indent()
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
    s.append(";")
  }
}

case class CFuncImpl(definition: CFuncDef, implementation: CAstElement*) extends CAstElement {
  override def generate(s: CGeneratorState): Unit = {
    definition.returnType.generate(s)
    s.append(" ").append(definition.name)
    Helpers.generate(s, definition.parameters: _*)
    s.append(" {").eol().incIndentation()
    Helpers.generate(s, implementation)
    s.decIndentation().indent().append("}")
  }
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
