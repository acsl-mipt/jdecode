package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object ArraySize {
  def apply(min: Long = 0, max: Long = 0): ArraySize = ArraySizeImpl(min, max)
}
