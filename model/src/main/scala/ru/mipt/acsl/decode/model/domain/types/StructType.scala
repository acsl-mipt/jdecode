package ru.mipt.acsl.decode.model.domain.types

trait StructType extends DecodeType {
  def fields: Seq[StructField]
}
