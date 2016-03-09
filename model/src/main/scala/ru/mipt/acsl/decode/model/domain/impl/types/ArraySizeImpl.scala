package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private case class ArraySizeImpl(min: Long = 0, max: Long = 0) extends ArraySize {
  require(min >= 0)
  require(max >= 0)
}
