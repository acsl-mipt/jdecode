package ru.mipt.acsl.decode.model.registry

import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.types.AbstractNameNamespaceInfoAware
import ru.mipt.acsl.decode.model.{HasInfo, LocalizedString, NamespaceAware, Referenceable}

/**
  * @author Artem Shein
  */
trait DecodeUnit extends HasInfo with Referenceable with NamespaceAware {
  def display: LocalizedString
  def namespace_=(ns: Namespace): Unit
}

object DecodeUnit {

  private class Impl(name: ElementName, namespace: Namespace, var display: LocalizedString,
                               info: LocalizedString)
    extends AbstractNameNamespaceInfoAware(name, namespace, info) with DecodeUnit

  def apply(name: ElementName, namespace: Namespace, display: LocalizedString = LocalizedString.empty,
            info: LocalizedString = LocalizedString.empty): DecodeUnit =
    new Impl(name, namespace, display, info)
}

