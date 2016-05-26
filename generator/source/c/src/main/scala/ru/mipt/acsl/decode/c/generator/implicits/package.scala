package ru.mipt.acsl.decode.c.generator

import java.io
import java.io.File

import com.google.common.base.CaseFormat
import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.decode.model.component.message.{EventMessage, MessageParameter, TmMessage}
import ru.mipt.acsl.decode.model.component.{Command, Component}
import ru.mipt.acsl.decode.model.expr.{ConstExpr, BigIntLiteral$}
import ru.mipt.acsl.decode.model.naming.{Fqn, HasName}
import ru.mipt.acsl.decode.model.types.{DecodeType, NativeType}
import ru.mipt.acsl.generator.c.ast.implicits._
import ru.mipt.acsl.generator.c.ast.{CConstType, CType, CAstElements => _, _}

package object implicits {

  implicit def hasName2HasNameHelper(hasName: HasName): HasNameHelper = HasNameHelper(hasName)
  implicit def string2StringHelper(str: String): StringHelper = StringHelper(str)
  implicit def cExpression2CExpressionHelper(cExpr: CExpression): CExpressionHelper = CExpressionHelper(cExpr)
  implicit def decodeType2DecodeTypeHelper(decodeType: DecodeType): DecodeTypeHelper = DecodeTypeHelper(decodeType)
  implicit def component2ComponentHelper(component: Component): ComponentHelper = ComponentHelper(component)
  implicit def file2FileHelper(file: File): FileHelper = FileHelper(file)

  implicit class TmMessageHelper(message: TmMessage) {

    def fullImplMethodName(rootComponent: Component, component: Component): String =
      rootComponent.prefixedTypeName.methodName("Write" + rootComponent.methodNamePart(message, component).capitalize + "Impl")

    def fullMethodName(rootComponent: Component, component: Component): String =
      rootComponent.prefixedTypeName.methodName("Write" + rootComponent.methodNamePart(message, component).capitalize)

  }

  implicit class TmEventMessageHelper(val eventMessage: EventMessage) {
    def fullMethodDef(component: Component, c: Component): CFuncDef = {
      val eventParam = CFuncParam("event", eventMessage.baseType.cType)
      val eventParams = eventParam +: eventMessage.fields.flatMap {
        case Right(e) =>
          val t = e.parameterType
          Seq(CFuncParam(e.cName, mapIfNotSmall(t.cType, t, (t: CType) => t.ptr.const)))
        case _ => Seq.empty
      }
      CFuncDef(eventMessage.fullMethodName(component, c), resultType, eventParams)
    }
  }

  implicit class CVarHelper(v: CVar) {
    def define(t: CType, init: Option[CExpression] = None, static: Boolean = false) = CVarDef(v.name, t, init, static)
  }

  implicit class NativeTypeHelper(n: NativeType) {

    private val varuintFqn = Fqn.newFromSource("decode.varuint")

    def isVaruintType: Boolean = n.fqn.equals(varuintFqn)
  }

  implicit class MessageParameterVarHelper(val mp: MessageParameter) {

    def varName: String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
      mp.path.toString.replaceAll("[\\.\\[\\]]", "_").replaceAll("__", "_"))

  }

  implicit class AstElementHelper(val el: CAstElement) {

    def line: CStatementLine = CStatementLine(el)

    def eol: CAstElements = Seq(el, CEol)

  }

  implicit class CTypeHelper(val ct: CType) {
    def const: CConstType = CConstType(ct)
  }

  implicit class CommandHelper(val command: Command) {

    def cFuncParameterTypes(component: Component): Seq[CType] = {
      component.ptrType +: command.parameters.map(p => {
        val t = p.parameterType
        mapIfNotSmall(t.cType, t, (ct: CType) => ct.ptr)
      })
    }

  }

  implicit class CAstElementsHelper(val els: CAstElements) {

    def protectDoubleInclude(filePath: String): CAstElements = {
      val uniqueName = "__" + filePath.split(io.File.separatorChar).map(p =>
        p.upperCamel2UpperUnderscore.replaceAll("\\.", "_")).mkString("_") + "__"
      "DO NOT EDIT! FILE IS AUTO GENERATED".comment.eol ++ Seq(CIfNDef(uniqueName), CEol, CDefine(uniqueName)) ++ els :+ CEndIf
    }

    def externC: CAstElements =
      Seq(CIfDef(cppDefine), CEol, CPlainText("extern \"C\" {"), CEol, CEndIf, CEol, CEol) ++ els ++
        Seq(CEol, CEol, CIfDef(cppDefine), CEol, CPlainText("}"), CEol, CEndIf)

    def eol: CAstElements = els :+ CEol
  }

}