package ru.mipt.acsl.decode.model.domain
package impl.types

import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
trait Parameter extends HasNameAndInfo {

  def paramTypeProxy: MaybeProxy[DecodeType]
  def paramType: DecodeType = paramTypeProxy.obj
  def unitProxy: Option[MaybeProxy[DecodeUnit]]
  def unit: Option[DecodeUnit] = unitProxy.map(_.obj)
}

object Parameter {
  def apply(name: ElementName, info: LocalizedString, paramType: MaybeProxy[DecodeType],
            unit: Option[MaybeProxy[DecodeUnit]]): Parameter =
    new ParameterImpl(name, info, paramType, unit)
}
