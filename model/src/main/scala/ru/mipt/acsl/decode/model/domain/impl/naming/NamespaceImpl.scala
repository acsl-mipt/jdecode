package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.impl.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.impl.types.DecodeType
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

import scala.collection.immutable

/**
  * @author Artem Shein
  */
private class NamespaceImpl(var name: ElementName, var info: LocalizedString, var parent: Option[Namespace],
                            var types: immutable.Seq[DecodeType], var units: immutable.Seq[DecodeUnit],
                            var subNamespaces: immutable.Seq[Namespace], var components: immutable.Seq[Component])
  extends Namespace