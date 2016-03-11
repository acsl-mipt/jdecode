package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, SubType, SubTypeRange}

/**
  * @author Artem Shein
  */
private class SubTypeImpl(name: ElementName, namespace: Namespace, info: LocalizedString,
                          val baseType: MaybeProxy[DecodeType], val range: Option[SubTypeRange])
  extends AbstractType(name, namespace, info) with SubType
