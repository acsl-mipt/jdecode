package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.component.message.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.StructType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Component extends HasInfo with Referenceable with HasOptionId with NamespaceAware {
  def statusMessages: immutable.Seq[StatusMessage]
  def eventMessages: immutable.Seq[EventMessage]
  def commands: immutable.Seq[Command]
  def subComponents: immutable.Seq[ComponentRef]
  override def namespace: Namespace
  def baseTypeProxy: Option[MaybeProxy[StructType]]
  def baseType: Option[StructType] = baseTypeProxy.map(_.obj)
  def eventMessages_=(e: immutable.Seq[EventMessage]): Unit
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
