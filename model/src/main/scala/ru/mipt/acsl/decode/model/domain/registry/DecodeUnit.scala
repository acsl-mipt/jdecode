package ru.mipt.acsl.decode.model.domain.registry

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.{HasInfo, LocalizedString, NamespaceAware, Referenceable}

/**
  * @author Artem Shein
  */
trait DecodeUnit extends HasInfo with Referenceable with NamespaceAware {
  def display: LocalizedString
  def namespace_=(ns: Namespace): Unit
}

object DecodeUnit {
  def apply(name: ElementName, namespace: Namespace, display: LocalizedString = LocalizedString.empty,
            info: LocalizedString = LocalizedString.empty): DecodeUnit =
    new DecodeUnitImpl(name, namespace, display, info)
}

