package ru.mipt.acsl.decode.c.generator.implicits.serialization

import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.decode.c.generator.implicits._
import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.component.message._
import ru.mipt.acsl.decode.model.component.{Component, message}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.{GenericTypeSpecialized, StructType}
import ru.mipt.acsl.generator.c.ast._
import ru.mipt.acsl.generator.c.ast.implicits._

/**
  * @author Artem Shein
  */
// todo: refactoring
private[generator] case class MessageParameterSerializationHelper(mp: MessageParameter) {

  import message._

  private def id[A](a: A): A = a

  def serializeCallCode(rootComponent: Component, component: Component): CAstElements = {
    val mpr = mp.ref(component)
    val sf = mpr.structField.get // must be a structField here

    var isPtr = !sf.typeUnit.t.isSmall // if current expr is pointer
    var expr: CExpression = component.parameterMethodName(mp, rootComponent).call()
    var astGen: Option[CAstElements => CAstElements] = None

    var t = sf.typeUnit.t

    for (next <- mpr.path) {
      next match {
        case Left(elementName) =>

          t = t.asInstanceOf[StructType].fields.find(_.name == elementName)
            .getOrElse(sys.error(s"field $elementName not found")).typeUnit.t

          val isNotSmall = !t.isSmall
          expr = expr.dotOrArrow(elementName.asMangledString._var, !isPtr).mapIf(isNotSmall, e => CParens(e.ref))
          isPtr = isNotSmall

        case Right(range) =>

          val arrayType = t.asInstanceOf[GenericTypeSpecialized] // fixme
          sys.error("not implemented")
          /*t = arrayType.baseType
          val arraySize = arrayType.size
          val rangeType: ArrayType = ArrayType(t.fqn.last, t.namespace, LocalizedString.empty, MaybeProxy(t),
            range.size(arraySize))

          val fold = astGen.getOrElse(id[CAstElements] _)
          val arrExpr = Some(expr)
          val rangeCType = rangeType.cType
          val astPtr = isPtr
          val isRangeFixed = range.max.isDefined && arraySize.min > range.max.get
          val sizeExpr = range.max.map(max =>
            if (isRangeFixed)
              CULongLiteral(max)
            else
              "PHOTON_MIN".call(CULongLiteral(max), "array"._var.dotOrArrow(size.v, !isPtr)))
            .getOrElse("array"._var.dotOrArrow(size.v, !isPtr))

          astGen = Some((inner: CAstElements) => fold(
            Seq(
              CVarDef("array", if (astPtr) rangeCType.ptr else rangeCType, arrExpr).line,
              CVarDef(size.name, size.t, Some(sizeExpr)).line) ++
            (if (!isRangeFixed) CAstElements(size.v.serializeCallCodeForArraySize.line) else CAstElements.empty) ++
            CAstElements(CIndent,
              CForStatement(Seq(CVarDef(i.name, i.t, Some(CULongLiteral(range.min)))),
                Seq(CLess(i.v, size.v)), Seq(CIncBefore(i.v)), inner).eol)))

          val isNotSmall = !t.isSmall
          expr = "array"._var.dotOrArrow(dataVar, !isPtr)("i"._var).mapIf(isNotSmall, e => CParens(e.ref))
          isPtr = isNotSmall*/

      }
    }
    astGen.getOrElse(id[CAstElements] _)(Seq(t.serializeCallCode(expr)._try.line))
  }

}
