package ru.mipt.acsl.decode.model.domain.types

/**
  * @author Artem Shein
  */
trait EnumType extends DecodeType {
  def isFinal: Boolean
  def extendsOrBaseType: Either[EnumType, DecodeType]
  def extendsTypeOption: Option[EnumType]
  def baseTypeOption: Option[DecodeType]
  def constants: Set[EnumConstant]
  override def toString: String = "EnumType" + super.toString
}
