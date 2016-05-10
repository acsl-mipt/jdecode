package ru.mipt.acsl.decode.model.domain.expr

/**
  * @author Artem Shein
  */
trait FloatLiteral extends ConstExpr {
  def value: Float
}
