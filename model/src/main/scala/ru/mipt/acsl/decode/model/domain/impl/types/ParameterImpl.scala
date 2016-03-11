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
// Components
private class ParameterImpl(name: ElementName, info: LocalizedString, val paramType: MaybeProxy[DecodeType],
                            val unit: Option[MaybeProxy[DecodeUnit]])
  extends AbstractNameAndOptionalInfoAware(name, info) with Parameter
