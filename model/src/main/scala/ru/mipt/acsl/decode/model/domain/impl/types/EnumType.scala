package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, EnumConstant, EnumType}

/**
  * @author Artem Shein
  */
object EnumType {
  def apply(name: ElementName, namespace: Namespace,
            extendsOrBaseType: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
            info: ElementInfo, constants: Set[EnumConstant], isFinal: Boolean): EnumType =
    new EnumTypeImpl(name, namespace, extendsOrBaseType, info, constants, isFinal)
}
