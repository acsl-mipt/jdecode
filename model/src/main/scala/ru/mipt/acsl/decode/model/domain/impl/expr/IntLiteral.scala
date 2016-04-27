package ru.mipt.acsl.decode.model.domain.impl.expr

import ru.mipt.acsl.decode.model.domain.pure.{expr => e}

/**
  * @author Artem Shein
  */
object IntLiteral {
  def apply(value: Int): e.IntLiteral = new IntLiteralImpl(value)
}
