package ru.mipt.acsl.generator.cpp.ast


import scala.collection.mutable
import scala.collection.immutable

/**
  * @author Artem Shein
  */

trait CppAstElement {
  def generate(s: CppGeneratorState): Unit
}

trait CppStatement extends CppAstElement

trait CppExpression extends CppAstElement

case class ExprStatement(expr: CppExpression) extends CppStatement {

  override def generate(s: CppGeneratorState): Unit = {
    expr.generate(s)
    s.append(";")
  }

}

object CppStatement {
  def apply(expr: CppExpression) = ExprStatement(expr)
}

class Comment(val text: String) extends CppAstElement {

  def generate(s: CppGeneratorState): Unit =
    s.append("/* ").append(text).append(" */")

}

object Comment {
  def apply(text: String): Comment = new Comment(text)
}

object Eol extends CppAstElement {

  def generate(s: CppGeneratorState): Unit = s.eol()

}

trait CppMacroAstElement extends CppAstElement

class CppDefine(val name: String, val value: Option[String]) extends CppMacroAstElement {

  def generate(s: CppGeneratorState): Unit = {
    s.append("#define ").append(name)
    value.foreach(s.append(" ").append)
    s.eol()
  }

}

object CppDefine {
  def apply(name: String) = new CppDefine(name, None)

  def apply(name: String, value: String) = new CppDefine(name, Some(value))
}

class CppPragma(val value: String) extends CppMacroAstElement {

  def generate(s: CppGeneratorState): Unit = {
    s.append("#pragma ").append(value).eol()
  }

}

object CppPragma {
  def apply(value: String) = new CppPragma(value)
}

object CppIfNDef {
  type Type = CppAstElements[CppAstElement] with CppMacroAstElement

  class CppIfNDefImpl(val name: String, elements: immutable.Seq[CppAstElement]) extends CppAstElements(elements)
  with CppMacroAstElement {

    override def generate(s: CppGeneratorState): Unit = {
      s.append("#ifndef ").append(name).eol()
      elements.foreach(stmt => {
        stmt.generate(s); s.eol()
      })
    }

  }

  def apply(name: String, statements: Seq[CppAstElement]): Type = new CppIfNDefImpl(name, statements.to[immutable.Seq])
}

class CppEndIf extends CppMacroAstElement {
  override def generate(s: CppGeneratorState): Unit = s.append("#endif")
}

object CppEndIf {
  val obj = new CppEndIf()

  def apply() = obj
}

class CppInclude(val path: String) extends CppMacroAstElement {
  override def generate(s: CppGeneratorState) {
    s.append("#include \"").append(path).append("\"")
  }
}

object CppInclude {
  def apply(path: String) = new CppInclude(path)
}

trait CppType extends CppAstElement {
  def ptr(): CppPtrType = CppPtrType(this)
}

case class CppPtrType(subType: CppType) extends CppType {
  override def generate(s: CppGeneratorState): Unit = {
    subType.generate(s)
    s.append("*")
  }
}

case class CppTypeDefStatement(name: String, t: CppType) extends CppAstElement {
  override def generate(s: CppGeneratorState): Unit = {
    s.append("typedef ")
    t.generate(s)
    s.append(" ").append(name).append(";").eol()
  }
}

class CppNativeType(name: String) extends CppTypeApplication(name) {
  override def generate(s: CppGeneratorState): Unit = {
    s.append(name)
  }
}

case object CppVoidType extends CppNativeType("void")

case object CppUnsignedCharType extends CppNativeType("unsigned char")

case object CppSignedCharType extends CppNativeType("signed char")

case object CppUnsignedShortType extends CppNativeType("unsigned short")

case object CppSignedShortType extends CppNativeType("signed short")

case object CppUnsignedIntType extends CppNativeType("unsigned int")

case object CppSignedIntType extends CppNativeType("signed int")

case object CppUnsignedLongType extends CppNativeType("unsigned long")

case object CppSignedLongType extends CppNativeType("signed long")

case object CppFloatType extends CppNativeType("float")

case object CppDoubleType extends CppNativeType("double")

class CppTypeApplication(val name: String) extends CppType {
  override def generate(s: CppGeneratorState): Unit = s.append(name)
}

object CppTypeApplication {
  def apply(name: String): CppTypeApplication = new CppTypeApplication(name)
}

class CEnumTypeDefConst(val name: String, val value: Long)

object CEnumTypeDefConst {
  def apply(name: String, value: Long) = new CEnumTypeDefConst(name, value)
}

trait CppTypeDef extends CppType

class CppEnumTypeDef(consts: Iterable[CEnumTypeDefConst], name: Option[String] = None) extends CppTypeDef {
  override def generate(s: CppGeneratorState): Unit = {
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

object CppEnumTypeDef {
  def apply(consts: Iterable[CEnumTypeDefConst]) = new CppEnumTypeDef(consts)
}

case class CStructTypeDefField(name: String, t: CppType) {

}

case class CppStructTypeDef(fields: Traversable[CStructTypeDefField], name: Option[String] = None) extends CppType {
  override def generate(s: CppGeneratorState): Unit = {
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

object RefType {
  def apply(t: CppType) = new CppType {
    override def generate(s: CppGeneratorState): Unit = { t.generate(s); s.append(" &") }
  }
}

class Parameter(val name: String, val t: CppType) {

  def generate(s: CppGeneratorState): Unit = {
    t.generate(s)
    s.append(s" $name")
  }

}

object Parameter {
  def apply(name: String, t: CppType) = new Parameter(name, t)
}

sealed abstract class Visibility(val name: String) {

  def generate(s: CppGeneratorState): Unit = s.append(name)

}

case object Public extends Visibility("public")

case object Private extends Visibility("private")

case object Protected extends Visibility("protected")

case class ClassMethodDef(name: String, returnType: CppType, params: mutable.Buffer[Parameter] = mutable.Buffer.empty,
                     static: Boolean = false, visibility: Visibility = Public,
                     implementation: CppStatements = CppStatements(), virtual: Boolean = false,
                          _abstract: Boolean = false) {

  assert(!static || !virtual)
  assert(!_abstract || virtual)
  assert(implementation.isEmpty || !virtual)

  def generate(s: CppGeneratorState): Unit = {
    if (static)
      s.append("static ")
    if (virtual)
      s.append("virtual ")
    returnType.generate(s)
    s.append(s" $name")
    Helpers.generate(s, params)
    if (implementation.nonEmpty) {
      Helpers.generate(s, implementation)
    } else {
      if (_abstract)
        s.append(" = 0")
      s.append(";")
    }
  }

}

class ClassFieldDef(val name: String, val t: CppType, val static: Boolean = false, val visibility: Visibility = Public) {
  def generateWithoutSemicolon(s: CppGeneratorState): Unit = {
    if (static)
      s.append("static ")
    t.generate(s)
    s.append(" ").append(name)
  }
  def generate(s: CppGeneratorState): Unit = {
    generateWithoutSemicolon(s)
    s.append(";")
  }
}

case class ClassFieldInit(name: String, t: CppType, value: CppExpression, static: Boolean = false) extends CppStatement {
  override def generate(s: CppGeneratorState): Unit = {
    if (static)
      s.append("static ")
    t.generate(s)
    s.append(" ").append(name).append(" = ")
    value.generate(s)
    s.append(";")
  }
}

object ClassFieldInit {
  def apply(name: String, definition: ClassFieldDef, value: CppExpression) = new ClassFieldInit(name, definition.t, value, definition.static)
}

object ClassFieldDef {
  def apply(name: String, t: CppType, static: Boolean = false, visibility: Visibility = Public) = new ClassFieldDef(name, t, static, visibility)
}

private object Helpers {
  def generate(s: CppGeneratorState, parameters: Seq[Parameter]): Unit = {
    s.append("(")
    var first = true
    parameters.foreach { p => if (first) first = false else s.append(", "); p.generate(s) }
    s.append(")")
  }
  def generate(s: CppGeneratorState, statements: CppStatements) = {
    s.append(" {").eol().incIndentation()
    statements.statements.foreach({ statement => s.indent(); statement.generate(s); s.eol() })
    s.decIndentation().indent().append("}")
  }
}

case class CppCase(expr: CppExpression, statements: CppStatements) extends CppAstElement {
  override def generate(s: CppGeneratorState): Unit = {
    s.indent()
    s.append("case ")
    expr.generate(s)
    s.append(":").incIndentation().eol()
    statements.generate(s)
    s.decIndentation()
  }
}

case class CppSwitch(expr: CppExpression, cases: Seq[CppCase], default: Option[CppStatements] = None) extends CppStatement {
  override def generate(s: CppGeneratorState): Unit = {
    s.append("switch (")
    expr.generate(s)
    s.append(") {").incIndentation().eol()
    cases.foreach({ c => c.generate(s); s.eol() })
    default.foreach({ statements => s.indent().append("default:").eol().incIndentation(); statements.generate(s); s.decIndentation().eol() })
    s.decIndentation().indent().append("}")
  }
}

case class ClassMethodImpl(name: String, returnType: CppType, parameters: Seq[Parameter], implementation: CppStatements, static: Boolean = false) extends CppAstElement {
  override def generate(s: CppGeneratorState): Unit = {
    if (static)
      s.append("static ")
    returnType.generate(s)
    s.append(" ").append(name)
    Helpers.generate(s, parameters)
    Helpers.generate(s, implementation)
  }
}

class ClassDef(val name: String, val methods: mutable.Buffer[ClassMethodDef], val _extends: Seq[String], val fields: Seq[ClassFieldDef]) extends CppAstElement {
  private def generateMethodsAndFieldsVisibility(s: CppGeneratorState, visibility: Visibility): Unit = {
    val visibleMethods = methods.filter(_.visibility == visibility)
    val visibleFields = fields.filter(_.visibility == visibility)
    if (visibleMethods.nonEmpty || visibleFields.nonEmpty) {
      visibility.generate(s)
      s.append(":").eol()
      s.incIndentation()
      visibleFields.foreach({ f => s.indent(); f.generate(s); s.eol() })
      if (visibleFields.nonEmpty && visibleMethods.nonEmpty)
        s.eol()
      val methodGen = (m: ClassMethodDef) => { s.indent(); m.generate(s); s.eol().eol() }
      visibleMethods.filter(_.virtual).foreach(methodGen)
      visibleMethods.filterNot(_.virtual).foreach(methodGen)
      s.decIndentation()
    }
  }

  override def generate(s: CppGeneratorState): Unit = {
    s.append(s"class $name")
    if (_extends.nonEmpty) {
      s.append(" : public ").append(_extends.mkString(", public "))
    }
    s.eol().indent().append("{").eol()
    if (methods.nonEmpty || fields.nonEmpty) {
      generateMethodsAndFieldsVisibility(s, Public)
      generateMethodsAndFieldsVisibility(s, Protected)
      generateMethodsAndFieldsVisibility(s, Private)
    }
    s.indent().append("};")
  }
}

object ClassDef {
  def apply(name: String, methods: Seq[ClassMethodDef], _extends: Seq[String] = Seq.empty,
            fields: Seq[ClassFieldDef] = Seq.empty) = new ClassDef(name, methods.to[mutable.Buffer], _extends, fields)
}

case class CppStringLiteral(value: String) extends CppExpression {
  override def generate(s: CppGeneratorState): Unit = s.append("\"").append(value.replace("\"", "\\\"")).append("\"")
}

case class CppIntLiteral(value: Int) extends CppExpression {
  override def generate(s: CppGeneratorState): Unit = s.append(value.toString)
}

case class MutableCppAstElements[A <: CppAstElement](statements: mutable.Buffer[A] = mutable.Buffer.empty) extends CppAstElement {
  def nonEmpty = statements.nonEmpty
  def isEmpty = statements.isEmpty
  def +=(el: A) = { statements += el }
  def ++=(elements: Seq[A]) = { statements ++= elements; this }
  override def generate(s: CppGeneratorState): Unit = statements.foreach(_.generate(s))
}

case class CppAstElements[+A <: CppAstElement](statements: immutable.Seq[A] = immutable.Seq.empty)
  extends CppAstElement {
  def nonEmpty = statements.nonEmpty
  def isEmpty = statements.isEmpty

  override def generate(s: CppGeneratorState): Unit = statements.foreach(_.generate(s))
}

class CppStatements(statements: CppStatement*) extends CppAstElements(statements.to[immutable.Seq]) {
  override def generate(s: CppGeneratorState) = {
    var first = true
    statements.foreach({ stmt =>
      if (first) first = false else s.eol()
      s.indent()
      stmt.generate(s);
    })
  }
}

object CppStatements {
  def apply(statements: CppStatement*) = new CppStatements(statements: _*)
}

abstract class AbstractMutableNamespace[A <: CppAstElement](val name: String, statements: mutable.Buffer[A])
  extends MutableCppAstElements[A](statements) {
  override def generate(s: CppGeneratorState): Unit = {
    s.append("namespace ").append(name).eol().append("{").eol()
    super.generate(s)
    s.append("}").eol()
  }
}

abstract class AbstractNamespace[+A <: CppAstElement](val name: String, statements: immutable.Seq[A])
  extends CppAstElements[A](statements) {
  override def generate(s: CppGeneratorState): Unit = {
    s.append("namespace ").append(name).eol().append("{").eol()
    super.generate(s)
    s.append("}").eol()
  }
}

object MutableNamespace {
  type Type = AbstractMutableNamespace[CppAstElement] with CppAstElement

  private class MutableNamespaceImpl(name: String, statements: mutable.Buffer[CppAstElement])
    extends AbstractMutableNamespace[CppAstElement](name, statements) with CppAstElement

  def apply(name: String, statements: CppAstElement*): Type = new MutableNamespaceImpl(name, statements.to[mutable.Buffer])
}

object Namespace {
  type Type = AbstractNamespace[CppAstElement] with CppAstElement

  private class NamespaceImpl(name: String, statements: immutable.Seq[CppAstElement])
    extends AbstractNamespace[CppAstElement](name, statements) with CppAstElement

  def apply(name: String, statements: CppAstElement*): Type = new NamespaceImpl(name, statements.to[immutable.Seq])
}

object CppNamespace {
  type Type = AbstractNamespace[CppStatement] with CppStatement

  private class CppNamespaceImpl(name: String, statements: immutable.Seq[CppStatement])
    extends AbstractNamespace[CppStatement](name, statements) with CppStatement

  def apply(name: String, statements: CppStatement*): Type = new CppNamespaceImpl(name, statements.to[immutable.Seq])
}

case class MethodCall(methodName: String, arguments: CppExpression*) extends CppExpression {
  override def generate(s: CppGeneratorState): Unit = {
    s.append(methodName).append("(")
    var first = true
    arguments.foreach{ arg => if (first) first = false else s.append(", "); arg.generate(s) }
    s.append(")")
  }
}

case class CppVar(name: String) extends CppExpression {
  override def generate(s: CppGeneratorState): Unit = s.append(name)
}

case class MacroCall(name: String, arguments: CppExpression*) extends CppExpression {
  override def generate(s: CppGeneratorState): Unit = {
    s.append(name).append("(")
    var first = true
    arguments.foreach{ arg => if (first) first = false else s.append(", "); arg.generate(s) }
    s.append(")")
  }
}

class FuncCall(name: String, arguments: CppExpression*) extends MethodCall(name, arguments: _*)

object FuncCall {
  def apply(name: String, arguments: CppExpression*) = new FuncCall(name, arguments: _*)
}

package object Aliases {
  type StatementBuffer[+A <: CppAstElement] = CppAstElements[A] with CppAstElement
}

case class Return(expression: CppExpression) extends CppStatement {
  override def generate(s: CppGeneratorState): Unit = {
    s.append("return ")
    expression.generate(s)
    s.append(";")
  }
}

case class CppIf(expression: CppExpression, thenStatements: CppStatements = CppStatements(), elseStatements: CppStatements = CppStatements()) extends CppStatement {
  override def generate(s: CppGeneratorState): Unit = {
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

case class CppVarDef(name: String, t: CppType, init: Option[CppExpression]) extends CppStatement {
  override def generate(s: CppGeneratorState): Unit = {
    t.generate(s)
    s.append(" ").append(name)
    init.foreach{s.append(" = "); _.generate(s)}
    s.append(";")
  }
}

object CppFile {
  type Type = CppAstElements[CppAstElement]

  private class CppFileImpl(statements: immutable.Seq[CppAstElement]) extends CppAstElements[CppAstElement](statements)

  def apply(statements: CppAstElement*): Type = new CppFileImpl(statements.to[immutable.Seq])
}


object File {
  type Type = CppAstElements[CppAstElement]

  private class FileImpl(statements: immutable.Seq[CppAstElement] = immutable.Seq.empty) extends CppAstElements[CppAstElement](statements)

  def apply(statements: CppAstElement*): Type = new FileImpl(statements.to[immutable.Seq])
}

class CppGeneratorState(var a: Appendable) {
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

  def append(s: String): CppGeneratorState = {
    advance(s)
    a.append(s); this
  }

  def eol(): CppGeneratorState = {
    a.append("\n")
    line += 1
    linePos = 0
    this
  }
}

object CppGeneratorState {
  def apply(a: Appendable) = new CppGeneratorState(a)
}
