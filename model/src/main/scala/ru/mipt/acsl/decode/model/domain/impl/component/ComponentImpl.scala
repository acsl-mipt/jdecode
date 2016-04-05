package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.impl.component.message.EventMessage
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.types.{AbstractNameNamespaceInfoAware, StructType}
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.message.StatusMessage
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

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
