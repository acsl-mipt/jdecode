package ru.mipt.acsl.decode.model.domain.expr

/**
  * @author Artem Shein
  */
case class FloatLiteral(v: Float) extends ConstExpr {
  override def toString: String = v.toString
}
