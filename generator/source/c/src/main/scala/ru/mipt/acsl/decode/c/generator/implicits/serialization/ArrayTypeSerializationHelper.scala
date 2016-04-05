package ru.mipt.acsl.decode.c.generator.implicits.serialization

import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.decode.model.domain.impl.types.ArrayType
import ru.mipt.acsl.generator.c.ast.{CEol, CIncBefore, CLess, CAstElements => _, _}
import ru.mipt.acsl.generator.c.ast.implicits._
import ru.mipt.acsl.decode.c.generator.implicits._

/**
  * @author Artem Shein
  */
private[generator] case class ArrayTypeSerializationHelper(t: ArrayType) {

  def serializeCodeForArrayElements(src: CExpression): CAstElements = {
    val baseType = t.baseType
    val dataExpr = t.dotOrArrow(src, dataVar)(i.v)
    val sizeExpr = t.dotOrArrow(src, size.v)
    Seq(CIndent, CForStatement(Seq(i.v.define(i.t, Some(CIntLiteral(0))), CComma, size.v.assign(sizeExpr)),
      Seq(CLess(i.v, size.v)), Seq(CIncBefore(i.v)),
      Seq(baseType.serializeCallCode(mapIfNotSmall(dataExpr, baseType, (expr: CExpression) => expr.ref)).line)), CEol)
  }

  def deserializeCodeForArrayElements(dest: CExpression): CAstElements = {
    val baseType = t.baseType
    val dataExpr = dest -> dataVar(i.v)
    Seq(CIndent, CForStatement(Seq(i.v.define(i.t, Some(CIntLiteral(0))), CComma, size.v.assign(dest -> size.v)),
      Seq(CLess(i.v, size.v)), Seq(CIncBefore(i.v)), Seq(baseType.deserializeCallCode(dataExpr.ref).line)), CEol)
  }
}