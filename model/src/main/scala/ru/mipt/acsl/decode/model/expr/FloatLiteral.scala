package ru.mipt.acsl.decode.model.expr

/**
  * @author Artem Shein
  */
trait FloatLiteral extends ConstExpr {
  def value: Float
}

object FloatLiteral {

  private class Impl(val value: Float) extends FloatLiteral {

    override def toString: String = value.toString

  }

  def apply(value: Float): FloatLiteral = new Impl(value)
}
