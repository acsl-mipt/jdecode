package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.impl.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.types.DecodeType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
object Namespace {
  def apply(name: ElementName, info: ElementInfo = ElementInfo.empty, parent: Option[Namespace] = None,
            types: immutable.Seq[DecodeType] = immutable.Seq.empty,
            units: immutable.Seq[DecodeUnit] = immutable.Seq.empty,
            subNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty,
            components: immutable.Seq[Component] = immutable.Seq.empty): Namespace =
    new NamespaceImpl(name, info, parent, types, units, subNamespaces, components)
}
