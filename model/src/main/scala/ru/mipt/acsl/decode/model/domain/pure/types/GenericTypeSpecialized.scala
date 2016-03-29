package ru.mipt.acsl.decode.model.domain.pure.types

/**
  * @author Artem Shein
  */
trait GenericTypeSpecialized extends DecodeType {
  def genericType: GenericType

  def genericTypeArguments: Seq[Option[DecodeType]]
}
