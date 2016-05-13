package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.{HasNameAndInfo, LocalizedString}
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.registry.DecodeUnit
import ru.mipt.acsl.decode.model.types.{DecodeType, TypeUnit}

/**
  * @author Artem Shein
  */
trait Parameter extends HasNameAndInfo {

  def typeUnit: TypeUnit

  def typeProxy: MaybeProxy[DecodeType] = typeUnit.typeProxy

  def parameterType: DecodeType = typeProxy.obj

  def unitProxy: Option[MaybeProxy[DecodeUnit]] = typeUnit.unitProxy

  def unit: Option[DecodeUnit] = unitProxy.map(_.obj)

}

object Parameter {

  private class Impl(name: ElementName, info: LocalizedString, val typeUnit: TypeUnit)
    extends AbstractNameInfoAware(name, info) with Parameter

  def apply(name: ElementName, info: LocalizedString, typeUnit: TypeUnit): Parameter =
    new Impl(name, info, typeUnit)
}
