package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.HasBaseType

trait ArrayType extends DecodeType with HasBaseType {
  def size: ArraySize

  def isFixedSize: Boolean = {
    val thisSize: ArraySize = size
    val maxLength: Long = thisSize.max
    thisSize.min == maxLength && maxLength != 0
  }
}

trait ArraySize {
  def min: Long
  def max: Long
}
