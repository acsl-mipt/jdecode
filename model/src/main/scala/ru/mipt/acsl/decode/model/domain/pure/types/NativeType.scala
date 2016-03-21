package ru.mipt.acsl.decode.model.domain.pure.types

trait NativeType extends DecodeType {
  override def toString: String = "NativeType" + super.toString
}
