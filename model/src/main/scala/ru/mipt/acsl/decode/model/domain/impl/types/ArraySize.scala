package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.pure.types.ArraySize

/**
  * @author Artem Shein
  */
object ArraySize {
  def apply(min: Long = 0, max: Long = 0): ArraySize = ArraySizeImpl(min, max)
}
