package ru.mipt.acsl.decode.model.domain.pure.expr

/**
  * @author Artem Shein
  */
trait IntLiteral extends ConstExpr {
  def value: Int
}
