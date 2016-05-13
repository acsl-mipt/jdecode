package ru.mipt.acsl.decode.model.expr

/**
  * @author Artem Shein
  */
trait IntLiteral extends ConstExpr {
  def value: Int
}

object IntLiteral {

  private class Impl(val value: Int) extends IntLiteral {
    override def toString: String = value.toString
  }

  def apply(value: Int): IntLiteral = new Impl(value)
}