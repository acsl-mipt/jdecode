package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit

/**
  * @author Artem Shein
  */
trait TypeUnit {

  def typeProxy: MaybeProxy[DecodeType]

  def t: DecodeType = typeProxy.obj

  def unitProxy: Option[MaybeProxy[DecodeUnit]]

  def unit: Option[DecodeUnit] = unitProxy.map(_.obj)

}

object TypeUnit {
  def apply(typeProxy: MaybeProxy[DecodeType], unitProxy: Option[MaybeProxy[DecodeUnit]]): TypeUnit =
    new TypeUnitImpl(typeProxy, unitProxy)
}
