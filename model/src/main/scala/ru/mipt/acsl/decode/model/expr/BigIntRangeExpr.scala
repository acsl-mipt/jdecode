package ru.mipt.acsl.decode.model.expr

/**
  * @author Artem Shein
  */
trait BigIntRangeExpr extends ConstExpr {

  def from: BigIntLiteral

  def to: BigIntLiteral

  override def exprStringRepr: String = s"range[$from,$to]"

}

object BigIntRangeExpr {

  private case class BigIntRangeExprImpl(from: BigIntLiteral, to: BigIntLiteral) extends BigIntRangeExpr

  def apply(from: BigIntLiteral, to: BigIntLiteral): BigIntRangeExpr = BigIntRangeExprImpl(from, to)

}