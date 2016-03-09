package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, TypeUnit}

/**
  * @author Artem Shein
  */
object TypeUnit {
  def apply(t: MaybeProxy[DecodeType], unit: Option[MaybeProxy[DecodeUnit]]): TypeUnit =
    new TypeUnitImpl(t, unit)
}
