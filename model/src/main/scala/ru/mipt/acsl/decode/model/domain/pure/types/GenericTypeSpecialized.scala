package ru.mipt.acsl.decode.model.domain.pure.types

/**
  * @author Artem Shein
  */
trait GenericTypeSpecialized {
  def genericType: GenericType

  def genericTypeArguments: Seq[Option[DecodeType]]
}
