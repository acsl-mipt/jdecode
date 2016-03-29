package ru.mipt.acsl.decode.model.domain.pure.types

/**
  * @author Artem Shein
  */
trait HasBaseType {
  def baseType: DecodeType
}
