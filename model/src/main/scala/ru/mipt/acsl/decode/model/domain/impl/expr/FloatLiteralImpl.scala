package ru.mipt.acsl.decode.model.domain.impl.expr

import ru.mipt.acsl.decode.model.domain.pure.{expr => e}

/**
  * @author Artem Shein
  */
private class FloatLiteralImpl(val value: Float) extends e.FloatLiteral {
  override def toString: String = value.toString
}
