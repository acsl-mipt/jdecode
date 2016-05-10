package ru.mipt.acsl.decode.model.domain.impl.expr

import ru.mipt.acsl.decode.model.domain.expr.IntLiteral

/**
  * @author Artem Shein
  */
object IntLiteral {
  def apply(value: Int): IntLiteral = new IntLiteralImpl(value)
}
