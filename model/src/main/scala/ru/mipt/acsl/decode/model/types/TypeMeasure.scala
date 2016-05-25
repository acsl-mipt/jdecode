package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.registry.Measure
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait TypeMeasure {

  def typeProxy: MaybeProxy[DecodeType]

  def t: DecodeType = typeProxy.obj

  def unitProxy: Option[MaybeProxy[Measure]]

  def unit: Option[Measure] = unitProxy.map(_.obj)

}

object TypeMeasure {

  private class Impl(val typeProxy: MaybeProxy[DecodeType], val unitProxy: Option[MaybeProxy[Measure]])
    extends TypeMeasure

  def apply(typeProxy: MaybeProxy[DecodeType], unitProxy: Option[MaybeProxy[Measure]]): TypeMeasure =
    new Impl(typeProxy, unitProxy)
}
