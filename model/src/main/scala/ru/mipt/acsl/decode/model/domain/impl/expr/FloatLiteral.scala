package ru.mipt.acsl.decode.model.domain.impl.expr

import ru.mipt.acsl.decode.model.domain.pure.{expr => e}

/**
  * @author Artem Shein
  */
object FloatLiteral {
  def apply(value: Float): e.FloatLiteral = new FloatLiteralImpl(value)
}
