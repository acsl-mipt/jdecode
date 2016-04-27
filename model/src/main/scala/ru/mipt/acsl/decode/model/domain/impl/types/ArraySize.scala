package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.pure.types

/**
  * @author Artem Shein
  */
object ArraySize {
  def apply(min: Long = 0, max: Long = 0): types.ArraySize = ArraySizeImpl(min, max)
}
