package ru.mipt.acsl.decode.model.domain.types

trait PrimitiveType extends DecodeType {
  def bitLength: Long

  def kind: TypeKind.Value
}
