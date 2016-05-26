package ru.mipt.acsl.decode.model.expr

/**
  * @author Artem Shein
  */
trait BigDecimalLiteral extends ConstExpr {
  def value: BigDecimal
}

object BigDecimalLiteral {

  private class Impl(val value: BigDecimal) extends BigDecimalLiteral {

    override def toString: String = value.toString

  }

  def apply(value: String): BigDecimalLiteral = new Impl(BigDecimal(value))
  def apply(value: BigDecimal): BigDecimalLiteral = new Impl(value)
}
