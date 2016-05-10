package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.component.message.EventMessage
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.component.message.StatusMessage
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractNameNamespaceInfoAware
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.StructType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
private class ComponentImpl(name: ElementName, namespace: Namespace, var id: Option[Int],
                            var baseTypeProxy: Option[MaybeProxy[StructType]], info: LocalizedString,
                            var subComponents: immutable.Seq[ComponentRef],
                            var commands: immutable.Seq[Command] = immutable.Seq.empty,
                            var eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
                            var statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty)
  extends AbstractNameNamespaceInfoAware(name, namespace, info) with Component
