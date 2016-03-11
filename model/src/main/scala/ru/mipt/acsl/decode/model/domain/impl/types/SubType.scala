package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, SubType, SubTypeRange}

/**
  * @author Artem Shein
  */
object SubType {
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            baseType: MaybeProxy[DecodeType], range: Option[SubTypeRange] = None): SubType =
    new SubTypeImpl(name, namespace, info, baseType, range)
}
