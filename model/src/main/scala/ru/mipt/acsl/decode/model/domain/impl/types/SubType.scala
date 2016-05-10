package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, SubTypeRange}

/**
  * @author Artem Shein
  */
trait SubType extends ru.mipt.acsl.decode.model.domain.types.SubType with DecodeType with HasBaseType {
  def baseTypeProxy: MaybeProxy[DecodeType]
  override def baseType: DecodeType = baseTypeProxy.obj
}

object SubType {
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            baseTypeProxy: MaybeProxy[DecodeType], range: Option[SubTypeRange] = None): SubType =
    new SubTypeImpl(name, namespace, info, baseTypeProxy, range)
}
