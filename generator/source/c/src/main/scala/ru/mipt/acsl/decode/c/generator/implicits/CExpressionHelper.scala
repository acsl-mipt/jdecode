package ru.mipt.acsl.decode.c.generator.implicits

import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.decode.model.domain.impl.types.DecodeType
import ru.mipt.acsl.generator.c.ast._

/**
  * @author Artem Shein
  */
private[generator] case class CExpressionHelper(expr: CExpression) {

  def _try: CFuncCall = tryMacroName.call(expr)

  def ->(expr2: CExpression): CArrow = CArrow(expr, expr2)

  def apply(indexExpr: CExpression): CIndex = CIndex(expr, indexExpr)

  def ref: CExpression = expr match {
    case expr: CDeref => expr.expr
    case _ => CRef(expr)
  }

  def refIfNotSmall(t: DecodeType): CExpression = if (t.isSmall) expr else expr.ref

  def mapIf(flag: Boolean, mapper: CExpression => CExpression): CExpression = if (flag) mapper(expr) else expr

  def derefIf(flag: Boolean): CExpression = mapIf(flag, _.deref)

  def derefIfSmall(t: DecodeType): CExpression = derefIf(t.isSmall)

  def assign(right: CExpression) = CAssign(expr, right)

  def dot(right: CExpression): CDot = CDot(expr, right)

  def dotOrArrow(expr2: CExpression, isDot: Boolean): CExpression = if (isDot) expr.dot(expr2) else expr.->(expr2)

  def deref = CDeref(expr)

  def cast(cType: CType): CTypeCast = CTypeCast(expr, cType)

}
