package ru.mipt.acsl.decode.model.domain.types

trait ArrayType extends DecodeType with HasBaseType {
  def size: ArraySize

  def isFixedSize: Boolean = {
    val thisSize: ArraySize = size
    val maxLength: Long = thisSize.max
    thisSize.min == maxLength && maxLength != 0
  }
}


