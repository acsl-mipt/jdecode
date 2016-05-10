package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.registry.DecodeUnit

/**
  * @author Artem Shein
  */
trait TypeUnit extends pure.types.TypeUnit {
  def typeProxy: MaybeProxy[DecodeType]
  override def t: DecodeType = typeProxy.obj
  def unitProxy: Option[MaybeProxy[DecodeUnit]]
  override def unit: Option[DecodeUnit] = unitProxy.map(_.obj)
}

object TypeUnit {
  def apply(typeProxy: MaybeProxy[DecodeType], unitProxy: Option[MaybeProxy[DecodeUnit]]): TypeUnit =
    new TypeUnitImpl(typeProxy, unitProxy)
}
