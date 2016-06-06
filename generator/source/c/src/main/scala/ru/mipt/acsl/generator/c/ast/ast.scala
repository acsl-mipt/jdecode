package ru.mipt.acsl.generator.c.ast

import ru.mipt.acsl.generator.c.ast.implicits.CAstElements

/**
  * @author Artem Shein
  */

trait CAstElement {

  def generate(s: CGenState): Unit

}

object CAstElements {
  def apply(elements: CAstElement*): CAstElements = Seq(elements: _*)
  def apply(): CAstElements = Seq.empty
  def empty: CAstElements = Seq.empty
}

package object implicits {
  type CAstElements = Seq[CAstElement]
  implicit class CAstElementsGeneratable(els: CAstElements) extends CAstElement {
    override def generate(s: CGenState): Unit = { Helpers.generate(s, els) }
  }
}

import implicits._

object CStatements {
  def apply(elements: CAstElement*): CAstElements = CAstElements(elements.map(CStatementLine(_)): _*)
}

trait CStatement extends CAstElement

trait CExpression extends CStatement

case class CExprStatement(expr: CExpression) extends CStatement {
  override def generate(s: CGenState): Unit = {
    expr.generate(s)
    s.append(";")
  }
}

object CStatement {
  def apply(expr: CExpression) = CExprStatement(expr)
}

class CComment(val text: String) extends CAstElement {
  override def generate(s: CGenState): Unit = {
    s.append("/* ").append(text).append(" */")
  }
}

object CComment {
  def apply(text: String): CComment = new CComment(text)
}

object CEol extends CAstElement {
  override def generate(s: CGenState): Unit = s.eol()
}

trait CMacroAstElement extends CAstElement

class CDefine(val name: String, val value: Option[String]) extends CMacroAstElement {
  override def generate(s: CGenState): Unit = {
    s.append("#define ").append(name)
    value.foreach(s.append(" ").append)
  }
}

object CDefine {
  def apply(name: String) = new CDefine(name, None)

  def apply(name: String, value: String) = new CDefine(name, Some(value))
}

class CPragma(val value: String) extends CMacroAstElement {
  override def generate(s: CGenState): Unit = {
    s.append("#pragma ").append(value).eol()
  }
}

object CPragma {
  def apply(value: String) = new CPragma(value)
}

case class CIfDef(name: String) extends CMacroAstElement {
  override def generate(s: CGenState): Unit = {
    s.append(s"#ifdef $name")
  }
}

case class CIfNDef(name: String) extends CMacroAstElement {
  override def generate(s: CGenState): Unit = {
    s.append(s"#ifndef $name")
  }
}

case object CEndIf extends CMacroAstElement {
  override def generate(s: CGenState): Unit = s.append("#endif")
}

case class CInclude(path: String) extends CMacroAstElement {
  override def generate(s: CGenState) {
    s.append("#include \"").append(path).append("\"")
  }
}

case class CPlainText(text: String) extends CAstElement {
  override def generate(s: CGenState): Unit = s.append(text)
}

trait CType extends CExpression {

  def ptr: CPtrType = CPtrType(this)

  def constPtr: CConstType = CConstType(ptr)

}

abstract class CNamedType(val name: String) extends CType

case class CArrayType(subType: CType, length: Long, _name: String) extends CNamedType(_name) {
  override def generate(s: CGenState): Unit = {
    subType.generate(s)
    s.append(" ").append(name).append("[").append(length.toString).append("]")
  }
}

case class CPtrType(subType: CType) extends CType {
  override def generate(s: CGenState): Unit = {
    subType.generate(s)
    s.append("*")
  }
}

case class CConstType(t: CType) extends CType {
  override def generate(s: CGenState): Unit = {
    s.append("const ")
    t.generate(s)
  }
}

case class CTypeDefStatement(name: String, t: CType) extends CAstElement {
  def toType: CType = CTypeApplication(name)
  def ptr: CPtrType = toType.ptr
  override def generate(s: CGenState): Unit = {
    s.append("typedef ")
    t.generate(s)
    s.append(" ").append(name).append(";").eol()
  }
}

case class CForStatement(init: CAstElements = CAstElements(), test: CAstElements = CAstElements(),
                         inc: CAstElements = CAstElements(), statements: CAstElements = CAstElements()) extends CStatement {
  override def generate(s: CGenState): Unit = {
    s.append("for(")
    init.generate(s)
    s.append("; ")
    test.generate(s)
    s.append("; ")
    inc.generate(s)
    s.append(") {").incIndentation().eol()
    statements.generate(s)
    s.decIndentation().indent().append("}")
  }
}

class CNativeType(name: String) extends CTypeApplication(name) {
  override def generate(s: CGenState): Unit = {
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

case object CInt8TType extends CNativeType("int8_t")
case object CInt16TType extends CNativeType("int16_t")
case object CInt32TType extends CNativeType("int32_t")
case object CInt64TType extends CNativeType("int64_t")
case object CUint8TType extends CNativeType("uint8_t")
case object CUint16TType extends CNativeType("uint16_t")
case object CUint32TType extends CNativeType("uint32_t")
case object CUint64TType extends CNativeType("uint64_t")

class CTypeApplication(val name: String) extends CType {
  override def generate(s: CGenState): Unit = s.append(name)
}

object CTypeApplication {
  def apply(name: String) = new CTypeApplication(name)
}

case class CEnumTypeDefConst(name: String, value: Long)

case class CFuncType(returnType: CType, parameterTypes: Iterable[CType], _name: String = "") extends CNamedType(_name) {
  override def generate(s: CGenState): Unit = {
    returnType.generate(s)
    s.append(s" (*$name)")
    Helpers.generate(s, parameterTypes)
  }
}

trait CTypeDef extends CType

case class CBlock(elements: CAstElement*) extends CAstElement {
  override def generate(s: CGenState): Unit = {
    s.append(" {").eol().incIndentation()
    elements.foreach { el => s.indent(); el.generate(s); s.append(";").eol() }
    s.decIndentation().indent().append("}")
  }
}

case class CEnumTypeDef(consts: Iterable[CEnumTypeDefConst], name: Option[String] = None) extends CTypeDef {
  override def generate(s: CGenState): Unit = {
    s.append("enum")
    name.foreach(s.append(" ").append)
    s.append(" {").eol().incIndentation()
    var first = true
    consts.map(c => {
      s.indent()
      if (first) { first = !first; s.append(" ") } else s.append(",")
      s.append(" ").append(c.name).append(" = ").append(c.value.toString).eol()
    })
    s.decIndentation().indent().append("}")
  }
}

object CEnumTypeDef {
  def apply(consts: CEnumTypeDefConst*) = new CEnumTypeDef(consts)
}

case class CStructType(name: String) extends CType {
  override def generate(s: CGenState): Unit = s.append(s"struct $name")
}

case class CStructTypeDefField(name: String, t: CType) extends CAstElement {
  override def generate(s: CGenState): Unit = {
    t match {
      case f: CNamedType => f.generate(s)
      case _ => t.generate(s); s.append(s" $name")
    }
  }
}

case class CForwardStructDecl(name: String) extends CAstElement {
  override def generate(s: CGenState): Unit = s.append(s"struct $name;")
}

case class CAssign(left: CExpression, right: CExpression) extends CExpression {
  override def generate(s: CGenState): Unit = {
    left.generate(s)
    s.append(" = ")
    right.generate(s)
  }
}

case class CStatementLine(elements: CAstElement*) extends CAstElement {
  override def generate(s: CGenState): Unit = {
    s.indent()
    Helpers.generate(s, elements)
    s.append(";").eol()
  }
}

case class CForwardStructTypeDef(name: String, structName: String) extends CStatement {
  override def generate(s: CGenState): Unit = {
    s.append(s"typedef struct $structName $name;")
  }
}

case class CStructTypeDef(fields: Traversable[CStructTypeDefField], name: Option[String] = None) extends CType {
  override def generate(s: CGenState): Unit = {
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

case class CFuncParam(name: String, t: CType) {

  def generate(s: CGenState): Unit = Helpers.generate(s, t, name)

}

private object Helpers {
  def generate(s: CGenState, t: CType, name: String): Unit = t match {
    case t: CNamedType => t.generate(s)
    case _ => t.generate(s); s.append(s" $name")
  }

  def generate(s: CGenState, parameterTypes: Iterable[CType]): Unit = {
    s.append("(")
    var first = true
    parameterTypes.foreach { p => if (first) first = false else s.append(", "); p.generate(s) }
    s.append(")")
  }

  implicit class Elements(val seq: Seq[CAstElement])

  def generate(s: CGenState, elements: Elements): Unit = {
    elements.seq.foreach(_.generate(s))
  }

  def generate(s: CGenState, parameters: CFuncParam*): Unit = {
    s.append("(")
    var first = true
    parameters.foreach { p => if (first) first = false else s.append(", "); p.generate(s) }
    s.append(")")
  }
}

case class CCase(expr: CExpression, statements: CAstElements) extends CAstElement {
  override def generate(s: CGenState): Unit = {
    s.indent()
    s.append("case ")
    expr.generate(s)
    s.append(":").incIndentation().eol()
    statements.generate(s)
    s.decIndentation()
  }
}

case class CSwitch(expr: CExpression, cases: Seq[CCase], default: CAstElements = CAstElements()) extends CStatement {
  override def generate(s: CGenState): Unit = {
    s.append("switch (")
    expr.generate(s)
    s.append(") {").incIndentation().eol()
    cases.foreach(_.generate(s))
    default.foreach({ statements => s.indent().append("default:").eol().incIndentation(); statements.generate(s); s.decIndentation() })
    s.decIndentation().indent().append("}")
  }
}

case class CStringLiteral(value: String) extends CExpression {
  override def generate(s: CGenState): Unit = s.append("\"").append(value.replace("\"", "\\\"")).append("\"")
}

case class CIntLiteral(value: Int) extends CExpression {
  override def generate(s: CGenState): Unit = s.append(value.toString)
}

case class CArrow(expr: CExpression, expr2: CExpression) extends CExpression {
  override def generate(s: CGenState): Unit = {
    expr.generate(s)
    s.append("->")
    expr2.generate(s)
  }
}

case class CFuncCall(methodName: String, arguments: CExpression*) extends CExpression {
  override def generate(s: CGenState): Unit = {
    s.append(methodName).append("(")
    var first = true
    arguments.foreach{ arg => if (first) first = false else s.append(", "); arg.generate(s) }
    s.append(")")
  }
}

case class CVar(name: String) extends CExpression {
  override def generate(s: CGenState): Unit = s.append(name)
}

case class CTypeCast(expr: CExpression, cType: CType) extends CExpression {
  override def generate(s: CGenState): Unit = {
    s.append("(")
    cType.generate(s)
    s.append(") ")
    expr.generate(s)
  }
}

abstract class CUnaryOp(private val expr: CExpression, val op: String, private val isBefore: Boolean = true) extends CExpression {
  override def generate(s: CGenState): Unit = {
    if (isBefore)
      s.append(op)
    expr.generate(s)
    if (!isBefore)
      s.append(op)
  }
}

case class CIncBefore(expr: CExpression) extends CUnaryOp(expr, "++")
case class CRef(expr: CExpression) extends CUnaryOp(expr, "&")
class CDeref(val expr: CExpression) extends CUnaryOp(expr, "*")

object CDeref {
  def apply(expr: CExpression) = expr match {
    case e: CRef => e.expr
    case _ => new CDeref(expr)
  }
}

case class CIndex(expr: CExpression, index: CExpression) extends CExpression {
  override def generate(s: CGenState): Unit = {
    expr.generate(s)
    s.append("[")
    index.generate(s)
    s.append("]")
  }
}

case class CParens(expr: CExpression) extends CExpression {
  override def generate(s: CGenState): Unit = {
    s.append("(")
    expr.generate(s)
    s.append(")")
  }
}

abstract class CBinaryOp(private val left: CExpression, val op: String, private val right: CExpression,
                         val isSpaced: Boolean = true) extends CExpression {
  override def generate(s: CGenState): Unit = {
    left.generate(s)
    if (isSpaced)
      s.append(s" $op ")
    else
      s.append(op)
    right.generate(s)
  }
}

case class CEq(left: CExpression, right: CExpression) extends CBinaryOp(left, "==", right)
case class CNotEq(left: CExpression, right: CExpression) extends CBinaryOp(left, "!=", right)
case class CLess(left: CExpression, right: CExpression) extends CBinaryOp(left, "<", right)
case class CPlus(left: CExpression, right: CExpression) extends CBinaryOp(left, "+", right)
case class CMul(left: CExpression, right: CExpression) extends CBinaryOp(left, "*", right)
case class CDot(left: CExpression, right: CExpression) extends CBinaryOp(left, ".", right, false)

case class MacroCall(name: String, arguments: CExpression*) extends CExpression with CStatement {
  override def generate(s: CGenState): Unit = {
    s.append(name).append("(")
    var first = true
    arguments.foreach{ arg => if (first) first = false else s.append(", "); arg.generate(s) }
    s.append(")")
  }
}

case class CReturn(expression: CExpression) extends CStatement {
  override def generate(s: CGenState): Unit = {
    s.append("return ")
    expression.generate(s)
  }
}

case class CIf(expression: CExpression, thenStatements: CAstElements = CAstElements(),
               elseStatements: CAstElements = CAstElements()) extends CStatement {
  override def generate(s: CGenState): Unit = {
    s.append("if (")
    expression.generate(s)
    s.append(")")
    s.incIndentation()
    Helpers.generate(s, thenStatements)
    s.decIndentation()
    if (elseStatements.nonEmpty) {
      s.append(" else")
      Helpers.generate(s, elseStatements)
    }
  }
}

case object CComma extends CAstElement {
  override def generate(s: CGenState): Unit = s.append(", ")
}

case class CVarDef(name: String, t: CType, init: Option[CExpression] = None, static: Boolean = false) extends CStatement {
  override def generate(s: CGenState): Unit = {
    if (static)
      s.append("static ")
    t.generate(s)
    s.append(" ").append(name)
    init.foreach(i => { s.append(" = "); i.generate(s) })
  }
}

case class CFuncDef(name: String, returnType: CType = CVoidType, parameters: Seq[CFuncParam] = Seq.empty) extends CAstElement {
  override def generate(s: CGenState): Unit = {
    returnType.generate(s)
    s.append(" ").append(name)
    Helpers.generate(s, parameters: _*)
    s.append(";")
  }
}

case object CSemicolon extends CAstElement {
  override def generate(s: CGenState): Unit = s.append(";")
}

case object CIndent extends CAstElement {
  override def generate(s: CGenState): Unit = s.indent()
}

case object CBreak extends CAstElement {
  override def generate(s: CGenState): Unit = s.append("break")
}

case class CFuncImpl(definition: CFuncDef, implementation: CAstElements = CAstElements()) extends CAstElement {
  override def generate(s: CGenState): Unit = {
    definition.returnType.generate(s)
    s.append(" ").append(definition.name)
    Helpers.generate(s, definition.parameters: _*)
    s.append(" {").eol().incIndentation()
    Helpers.generate(s, implementation)
    s.decIndentation().indent().append("}")
  }
}

class CGenState(var a: Appendable) {
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

  def append(s: String): CGenState = {
    advance(s)
    a.append(s); this
  }

  def eol(): CGenState = {
    a.append("\n")
    line += 1
    linePos = 0
    this
  }
}

object CGenState {
  def apply(a: Appendable) = new CGenState(a)
}
