package ru.mipt.acsl.decode.model.domain.impl.expr

import ru.mipt.acsl.decode.model.domain.pure.expr.FloatLiteral

/**
  * @author Artem Shein
  */
object FloatLiteral {
  def apply(value: Float): FloatLiteral = new FloatLiteralImpl(value)
}
