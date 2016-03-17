package ru.mipt.acsl.decode.model.domain.pure.types

/**
  * @author Artem Shein
  */
trait EnumType extends DecodeType {
  def isFinal: Boolean
  def extendsOrBaseType: Either[EnumType, DecodeType]
  def extendsType: Option[EnumType]
  def constants: Set[EnumConstant]
}
