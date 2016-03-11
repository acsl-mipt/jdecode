package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.component.Parameter
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
object Parameter {
  def apply(name: ElementName, info: LocalizedString, paramType: MaybeProxy[DecodeType],
            unit: Option[MaybeProxy[DecodeUnit]]): Parameter =
    new ParameterImpl(name, info, paramType, unit)
}
