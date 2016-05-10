package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, HasName}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
trait AliasType extends HasBaseType with DecodeType with HasName

object AliasType {
  def apply(name: ElementName, namespace: Namespace, baseTypeProxy: MaybeProxy[DecodeType],
            info: LocalizedString): AliasType = new AliasTypeImpl(name, namespace, info, baseTypeProxy)
}
