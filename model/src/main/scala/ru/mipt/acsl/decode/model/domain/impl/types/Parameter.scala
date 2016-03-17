package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
trait Parameter extends pure.Parameter {
  def paramTypeProxy: MaybeProxy[DecodeType]
  override def paramType: DecodeType = paramTypeProxy.obj
  def unitProxy: Option[MaybeProxy[DecodeUnit]]
  override def unit: Option[DecodeUnit] = unitProxy.map(_.obj)
}

object Parameter {
  def apply(name: ElementName, info: LocalizedString, paramType: MaybeProxy[DecodeType],
            unit: Option[MaybeProxy[DecodeUnit]]): Parameter =
    new ParameterImpl(name, info, paramType, unit)
}
