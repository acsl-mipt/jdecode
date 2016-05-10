package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.{LocalizedString, pure}
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.SubTypeRange

/**
  * @author Artem Shein
  */
trait SubType extends pure.types.SubType with DecodeType with HasBaseType {
  def baseTypeProxy: MaybeProxy[DecodeType]
  override def baseType: DecodeType = baseTypeProxy.obj
}

object SubType {
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            baseTypeProxy: MaybeProxy[DecodeType], range: Option[SubTypeRange] = None): SubType =
    new SubTypeImpl(name, namespace, info, baseTypeProxy, range)
}
