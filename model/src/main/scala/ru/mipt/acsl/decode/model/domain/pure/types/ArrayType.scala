package ru.mipt.acsl.decode.model.domain.pure.types

/**
  * @author Artem Shein
  */
trait ArrayType extends DecodeType with HasBaseType {
  def size: ArraySize
}
