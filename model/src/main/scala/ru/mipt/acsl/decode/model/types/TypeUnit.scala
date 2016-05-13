package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.registry.DecodeUnit
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

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

  private class Impl(val typeProxy: MaybeProxy[DecodeType], val unitProxy: Option[MaybeProxy[DecodeUnit]])
    extends TypeUnit

  def apply(typeProxy: MaybeProxy[DecodeType], unitProxy: Option[MaybeProxy[DecodeUnit]]): TypeUnit =
    new Impl(typeProxy, unitProxy)
}
