package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
private[domain] class ParameterImpl(name: ElementName, info: LocalizedString,
                            val paramTypeProxy: MaybeProxy[DecodeType],
                            val unitProxy: Option[MaybeProxy[DecodeUnit]])
  extends AbstractNameInfoAware(name, info) with Parameter
