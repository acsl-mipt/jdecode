package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.Namespace
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait SubType extends DecodeType with HasBaseType {

  def range: Option[SubTypeRange]

  def baseTypeProxy: MaybeProxy[DecodeType]

  override def baseType: DecodeType = baseTypeProxy.obj
}

object SubType {

  private class Impl(name: ElementName, namespace: Namespace, info: LocalizedString,
                     val baseTypeProxy: MaybeProxy[DecodeType], val range: Option[SubTypeRange])
    extends AbstractType(name, namespace, info) with SubType

  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            baseTypeProxy: MaybeProxy[DecodeType], range: Option[SubTypeRange] = None): SubType =
    new Impl(name, namespace, info, baseTypeProxy, range)
}
