package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object TypeUnit {
  def apply(t: MaybeProxy[DecodeType], unit: Option[MaybeProxy[DecodeUnit]]): TypeUnit =
    new TypeUnitImpl(t, unit)
}
