package ru.mipt.acsl.decode.model.domain.impl.expr

import ru.mipt.acsl.decode.model.domain.pure.{expr => e}

/**
  * @author Artem Shein
  */
private class IntLiteralImpl(val value: Int) extends e.IntLiteral {
  override def toString: String = value.toString
}
