package ru.mipt.acsl.decode.model.domain.pure.types

trait StructType extends DecodeType {
  def fields: Seq[StructField]
}
