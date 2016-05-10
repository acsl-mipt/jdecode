package ru.mipt.acsl.decode.model.domain.expr

/**
  * @author Artem Shein
  */
trait IntLiteral extends ConstExpr {
  def value: Int
}
