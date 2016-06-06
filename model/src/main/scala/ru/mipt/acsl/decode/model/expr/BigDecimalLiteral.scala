package ru.mipt.acsl.decode.model.expr

/**
  * @author Artem Shein
  */
trait BigDecimalLiteral extends ConstExpr {
  def value: BigDecimal
}

object BigDecimalLiteral {

  private class BigDecimalLiteralImpl(val value: BigDecimal) extends BigDecimalLiteral {

    override def toString: String = value.toString

  }

  def apply(value: String): BigDecimalLiteral = new BigDecimalLiteralImpl(BigDecimal(value))
  def apply(value: BigDecimal): BigDecimalLiteral = new BigDecimalLiteralImpl(value)
}
