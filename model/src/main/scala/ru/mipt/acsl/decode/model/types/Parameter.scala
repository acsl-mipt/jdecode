package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.registry.Measure
import ru.mipt.acsl.decode.model.{HasNameAndInfo, LocalizedString}

/**
  * @author Artem Shein
  */
trait Parameter extends HasNameAndInfo {

  def typeUnit: TypeMeasure

  def typeProxy: MaybeProxy[DecodeType] = typeUnit.typeProxy

  def parameterType: DecodeType = typeProxy.obj

  def unitProxy: Option[MaybeProxy[Measure]] = typeUnit.unitProxy

  def unit: Option[Measure] = unitProxy.map(_.obj)

}

object Parameter {

  private class Impl(name: ElementName, info: LocalizedString, val typeUnit: TypeMeasure)
    extends AbstractNameInfoAware(name, info) with Parameter

  def apply(name: ElementName, info: LocalizedString, typeUnit: TypeMeasure): Parameter =
    new Impl(name, info, typeUnit)
}
