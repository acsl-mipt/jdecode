package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{AliasType, DecodeType, HasBaseType}

/**
  * @author Artem Shein
  */
private class AliasTypeImpl(name: ElementName, namespace: Namespace, val baseType: MaybeProxy[DecodeType],
                            info: LocalizedString)
  extends AbstractType(name, namespace, info) with AliasType with HasBaseType {
  def optionName = Some(name)
}
