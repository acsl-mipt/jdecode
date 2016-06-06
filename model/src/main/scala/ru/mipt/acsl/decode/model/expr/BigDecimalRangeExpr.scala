package ru.mipt.acsl.decode.model.expr

/**
  * @author Artem Shein
  */
trait BigDecimalRangeExpr {

  def from: BigDecimalLiteral

  def to: BigDecimalLiteral

}

object BigDecimalRangeExpr {

  private case class BigDecimalRangeExprImpl(from: BigDecimalLiteral, to: BigDecimalLiteral) extends BigDecimalRangeExpr

  def apply(from: BigDecimalLiteral, to: BigDecimalLiteral): BigDecimalRangeExpr = BigDecimalRangeExprImpl(from, to)

}
