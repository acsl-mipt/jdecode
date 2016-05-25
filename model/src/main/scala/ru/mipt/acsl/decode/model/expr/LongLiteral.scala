package ru.mipt.acsl.decode.model.expr

/**
  * @author Artem Shein
  */
trait LongLiteral extends ConstExpr {
  def value: Long
}

object LongLiteral {

  private case class Impl(value: Long) extends LongLiteral {
    override def toString: String = value.toString
  }

  def apply(value: Long): LongLiteral = new Impl(value)
}