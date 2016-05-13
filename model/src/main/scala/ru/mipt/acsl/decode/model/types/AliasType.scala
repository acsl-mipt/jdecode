package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.{HasName, Namespace}
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.DecodeType

/**
  * @author Artem Shein
  */
trait AliasType extends HasBaseType with DecodeType with HasName

object AliasType {

  private class Impl(name: ElementName, namespace: Namespace, info: LocalizedString,
                     baseTypeProxy: MaybeProxy[DecodeType])
    extends AbstractBaseTypedType(name, namespace, info, baseTypeProxy) with AliasType

  def apply(name: ElementName, namespace: Namespace, baseTypeProxy: MaybeProxy[DecodeType],
            info: LocalizedString): AliasType = new Impl(name, namespace, info, baseTypeProxy)
}
