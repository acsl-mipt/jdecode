package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.{LocalizedString, pure}
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
trait AliasType extends BaseTypedType with pure.types.AliasType

object AliasType {
  def apply(name: ElementName, namespace: Namespace, baseTypeProxy: MaybeProxy[DecodeType],
            info: LocalizedString): AliasType = new AliasTypeImpl(name, namespace, info, baseTypeProxy)
}
