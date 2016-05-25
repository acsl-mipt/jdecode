package ru.mipt.acsl.decode.model.registry

import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.types.AbstractNameNamespaceInfoAware
import ru.mipt.acsl.decode.model.{HasInfo, LocalizedString, NamespaceAware, Referenceable}

/**
  * @author Artem Shein
  */
trait Measure extends HasInfo with Referenceable with NamespaceAware {
  def display: LocalizedString
  def namespace_=(ns: Namespace): Unit
}

object Measure {

  private class Impl(name: ElementName, namespace: Namespace, var display: LocalizedString,
                               info: LocalizedString)
    extends AbstractNameNamespaceInfoAware(name, namespace, info) with Measure

  def apply(name: ElementName, namespace: Namespace, display: LocalizedString = LocalizedString.empty,
            info: LocalizedString = LocalizedString.empty): Measure =
    new Impl(name, namespace, display, info)
}

