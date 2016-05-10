package ru.mipt.acsl.decode.model.domain.types

/**
  * @author Artem Shein
  */
trait HasBaseType {
  def baseType: DecodeType
}
