package ru.mipt.acsl.decode.model.domain.impl.expr

import ru.mipt.acsl.decode.model.domain.pure.expr.IntLiteral

/**
  * @author Artem Shein
  */
private class IntLiteralImpl(val value: Int) extends IntLiteral {
  override def toString: String = value.toString
}
