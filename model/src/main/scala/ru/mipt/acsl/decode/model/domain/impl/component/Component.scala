package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.impl.component.message.EventMessage
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.types.StructType
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.messages.StatusMessage
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Component extends pure.component.Component {
  override def namespace: Namespace
  def baseTypeProxy: Option[MaybeProxy[StructType]]
  override def baseType: Option[StructType] = baseTypeProxy.map(_.obj)
  override def subComponents: immutable.Seq[ComponentRef]
  override def commands: immutable.Seq[Command]
  override def eventMessages: immutable.Seq[EventMessage]
  def eventMessages_=(e: immutable.Seq[EventMessage]): Unit
  override def statusMessages: immutable.Seq[StatusMessage]
  def statusMessages_=(s: immutable.Seq[StatusMessage]): Unit
  def namespace_=(ns: Namespace): Unit
}

object Component {
  def apply(name: ElementName, namespace: Namespace, id: Option[Int], baseTypeProxy: Option[MaybeProxy[StructType]],
            info: LocalizedString, subComponents: immutable.Seq[ComponentRef],
            commands: immutable.Seq[Command] = immutable.Seq.empty,
            eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
            statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty): Component =
    new ComponentImpl(name, namespace, id, baseTypeProxy, info, subComponents, commands, eventMessages, statusMessages)
}
