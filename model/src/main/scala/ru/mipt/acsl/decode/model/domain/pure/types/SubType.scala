package ru.mipt.acsl.decode.model.domain.pure.types

trait SubType extends DecodeType with HasBaseType {
  def range: Option[SubTypeRange]
}
