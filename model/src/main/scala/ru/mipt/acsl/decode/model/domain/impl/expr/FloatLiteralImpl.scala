package ru.mipt.acsl.decode.model.domain.impl.expr

import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.expr.FloatLiteral

/**
  * @author Artem Shein
  */
private class FloatLiteralImpl(val value: Float) extends FloatLiteral {
  override def toString: String = value.toString
}
