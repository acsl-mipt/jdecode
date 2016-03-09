package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.messages.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.domain.component.{Command, Component, ComponentRef}
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.StructType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
object Component {
  def apply(name: ElementName, namespace: Namespace, id: Option[Int], baseType: Option[MaybeProxy[StructType]],
            info: ElementInfo, subComponents: immutable.Seq[ComponentRef],
            commands: immutable.Seq[Command] = immutable.Seq.empty,
            eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
            statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty): Component =
    new ComponentImpl(name, namespace, id, baseType, info, subComponents, commands, eventMessages, statusMessages)
}
