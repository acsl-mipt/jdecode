package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.types.DecodeType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
private[domain] class NamespaceImpl(var name: ElementName, var info: LocalizedString, var parent: Option[Namespace],
                            var types: immutable.Seq[DecodeType], var units: immutable.Seq[DecodeUnit],
                            var subNamespaces: immutable.Seq[Namespace], var components: immutable.Seq[Component])
  extends Namespace