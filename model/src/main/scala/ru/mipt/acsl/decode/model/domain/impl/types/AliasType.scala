package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{AliasType, DecodeType}

/**
  * @author Artem Shein
  */
object AliasType {
  def apply(name: ElementName, namespace: Namespace, baseType: MaybeProxy[DecodeType],
            info: ElementInfo): AliasType = new AliasTypeImpl(name, namespace, baseType, info)
}
