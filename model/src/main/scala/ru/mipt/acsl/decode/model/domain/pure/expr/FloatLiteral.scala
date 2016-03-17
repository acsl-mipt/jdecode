package ru.mipt.acsl.decode.model.domain.pure.expr

/**
  * @author Artem Shein
  */
trait FloatLiteral extends ConstExpr {
  def value: Float
}
