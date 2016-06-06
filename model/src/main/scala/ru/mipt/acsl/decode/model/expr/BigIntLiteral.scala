package ru.mipt.acsl.decode.model.expr

/**
  * @author Artem Shein
  */
trait BigIntLiteral extends ConstExpr {

  def value: BigInt

}

object BigIntLiteral {

  val MinLongValue = BigInt(Long.MinValue)
  val MaxLongValue = BigInt(Long.MaxValue)
  val MinULongValue = BigInt(0)
  val MaxULongValue = BigInt("18446744073709551615")

  private case class BigIntLiteralImpl(value: BigInt) extends BigIntLiteral {

    override def toString: String = value.toString

  }

  def apply(value: String): BigIntLiteral = new BigIntLiteralImpl(BigInt(value))

  def apply(value: BigInt): BigIntLiteral = new BigIntLiteralImpl(value)
}