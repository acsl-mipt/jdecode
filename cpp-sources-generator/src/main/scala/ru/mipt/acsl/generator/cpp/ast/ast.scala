package ru.mipt.acsl.generator.cpp.ast

import ru.mipt.acsl.generation.Generatable

import scala.collection.generic.Growable
import scala.collection.mutable
import scala.collection.immutable

/**
  * @author Artem Shein
  */

trait CppAstElement extends Generatable[CppGeneratorState]

trait CppStatement extends CppAstElement

trait Statement extends CppStatement

class Comment(val text: String) extends Statement {
  override def generate(s: CppGeneratorState): Unit = {
    s.append("/* ").append(text).append(" */")
  }
}

object Comment {
  def apply(text: String): Comment = new Comment(text)
}

object Eol extends Statement {
  override def generate(s: CppGeneratorState): Unit = s.eol()
}

trait CppMacroStatement extends Statement

class CppDefine(val name: String, val value: Option[String]) extends CppMacroStatement {
  override def generate(s: CppGeneratorState): Unit = {
    s.append("#define ").append(name)
    value.map(s.append(" ").append)
    s.eol()
  }
}

object CppDefine {
  def apply(name: String) = new CppDefine(name, None)

  def apply(name: String, value: String) = new CppDefine(name, Some(value))
}

object CppIfNDef {
  type Type = CppStatementBuffer[CppStatement] with CppMacroStatement

  class Impl(val name: String, statements: immutable.Seq[CppStatement]) extends CppStatementBuffer(statements)
  with CppMacroStatement {
    override def generate(s: CppGeneratorState): Unit = {
      s.append("#ifndef ").append(name).eol()
      statements.foreach(stmt => {
        stmt.generate(s); s.eol()
      })
    }
  }

  def apply(name: String, statements: Seq[CppStatement]): Type = new Impl(name, statements.to[immutable.Seq])
}

class CppEndIf extends CppMacroStatement {
  override def generate(s: CppGeneratorState): Unit = s.append("#endif")
}

object CppEndIf {
  val obj = new CppEndIf()

  def apply() = obj
}

class CppInclude(val path: String) extends CppMacroStatement {
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

case class CppTypeDefStatement(name: String, t: CppType) extends Statement {
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

class CEnumTypeDefConst(val name: String, val value: Int)

object CEnumTypeDefConst {
  def apply(name: String, value: Int) = new CEnumTypeDefConst(name, value)
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

trait Type extends Generatable[CppGeneratorState]

object Type {
  def apply(value: String) = new Type {
    override def generate(s: CppGeneratorState): Unit = s.append(value)
  }
}

class Parameter(val name: String, val t: Type) extends Generatable[CppGeneratorState] {
  override def generate(s: CppGeneratorState): Unit = {
    t.generate(s)
    s.append(s" $name")
  }
}

object Parameter {
  def apply(name: String, t: Type) = new Parameter(name, t)
}

sealed abstract class Visibility(val name: String) extends Generatable[CppGeneratorState] {
  override def generate(s: CppGeneratorState): Unit = s.append(name)
}

case object Public extends Visibility("public")

case object Private extends Visibility("private")

case object Protected extends Visibility("protected")

class ClassMethodDef(val name: String, val returnType: Type, val params: mutable.Buffer[Parameter],
                     val static: Boolean = false, val visibility: Visibility = Public)
  extends Generatable[CppGeneratorState] {
  override def generate(s: CppGeneratorState): Unit = {
    if (static)
      s.append("static ")
    returnType.generate(s)
    s.append(s" $name(")
    var first = true
    params.foreach { p => if (first) first = false else s.append(", "); p.generate(s) }
    s.append(");")
  }
}

object ClassMethodDef {
  def apply(name: String, returnType: Type, params: Seq[Parameter], static: Boolean = false) = new ClassMethodDef(name, returnType, params.to[mutable.Buffer], static)
}

class ClassDef(val name: String, val methods: mutable.Buffer[ClassMethodDef], val _extends: Seq[String]) extends Statement {
  private def generateMethodsVisibility(s: CppGeneratorState, visibility: Visibility): Unit = {
    val visibleMethods = methods.filter(_.visibility == visibility)
    if (visibleMethods.nonEmpty) {
      visibility.generate(s)
      s.append(":").eol()
      s.incIndentation()
      visibleMethods.foreach({ m => s.indent(); m.generate(s); s.eol() })
      s.decIndentation()
    }
  }

  override def generate(s: CppGeneratorState): Unit = {
    s.append(s"class $name")
    if (_extends.nonEmpty) {
      s.append(" : public ").append(_extends.mkString(", public "))
    }
    s.eol().indent().append("{").eol()
    if (methods.nonEmpty) {
      generateMethodsVisibility(s, Public)
      generateMethodsVisibility(s, Protected)
      generateMethodsVisibility(s, Private)
    }
    s.indent().append("};")
  }
}

object ClassDef {
  def apply(name: String, methods: Seq[ClassMethodDef], _extends: Seq[String] = Seq.empty) = new ClassDef(name, methods.to[mutable.Buffer], _extends)
}

abstract class CppStatementBuffer[+A <: CppStatement](val statements: immutable.Seq[A] = immutable.Seq.empty)
  extends CppStatement {
  def nonEmpty = statements.nonEmpty

  override def generate(s: CppGeneratorState): Unit = statements.foreach(_.generate(s))
}

abstract class AbstractNamespace[+A <: CppStatement](val name: String, statements: immutable.Seq[A])
  extends CppStatementBuffer[A](statements) {
  override def generate(s: CppGeneratorState): Unit = {
    s.append("namespace ").append(name).eol().append("{").eol()
    super.generate(s)
    s.append("}").eol()
  }
}

package object Aliases {
  type StatementBuffer[+A <: Statement] = CppStatementBuffer[A] with Statement
}

object Namespace {
  type Type = AbstractNamespace[Statement] with Statement

  private class Impl(name: String, statements: immutable.Seq[Statement])
    extends AbstractNamespace[Statement](name, statements) with Statement

  def apply(name: String, statements: Seq[Statement] = Seq.empty): Type = new Impl(name, statements.to[immutable.Seq])
}

object CppNamespace {
  type Type = AbstractNamespace[CppStatement] with CppStatement

  private class Impl(name: String, statements: immutable.Seq[CppStatement])
    extends AbstractNamespace[CppStatement](name, statements) with CppStatement

  def apply(name: String, statements: CppStatement*): Type = new Impl(name, statements.to[immutable.Seq])
}

object CppFile {
  type Type = CppStatementBuffer[CppStatement]

  private class Impl(statements: immutable.Seq[CppStatement]) extends CppStatementBuffer[CppStatement](statements)

  def apply(statements: Seq[CppStatement]): Type = new Impl(statements.to[immutable.Seq])
}


object File {
  type Type = CppStatementBuffer[Statement]

  private class Impl(statements: immutable.Seq[Statement] = immutable.Seq.empty) extends CppStatementBuffer[Statement](statements)

  def apply(): Type = new Impl()

  def apply(statements: Statement*): Type = new Impl(statements.to[immutable.Seq])
}

class CppGeneratorState(var a: Appendable) {
  var pos: Int = 0
  var line: Int = 0
  var linePos: Int = 0
  private var indentation: Int = 0

  def indent() = append("  " * indentation)

  def decIndentation() = indentation -= 1

  def incIndentation() = indentation += 1

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
