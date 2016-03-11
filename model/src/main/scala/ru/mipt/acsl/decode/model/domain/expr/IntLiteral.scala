package ru.mipt.acsl.decode.model.domain.expr

/**
  * @author Artem Shein
  */
case class IntLiteral(v: Int) extends ConstExpr {
  override def toString: String = v.toString
}
