package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object Parameter {
  def apply(name: ElementName, info: Option[String], paramType: MaybeProxy[DecodeType],
            unit: Option[MaybeProxy[DecodeUnit]]): Parameter =
    new ParameterImpl(name, info, paramType, unit)
}
