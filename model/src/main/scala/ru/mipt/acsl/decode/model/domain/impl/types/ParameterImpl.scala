package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private class ParameterImpl(name: ElementName, info: LocalizedString,
                            val paramTypeProxy: MaybeProxy[DecodeType],
                            val unitProxy: Option[MaybeProxy[DecodeUnit]])
  extends AbstractNameInfoAware(name, info) with Parameter
