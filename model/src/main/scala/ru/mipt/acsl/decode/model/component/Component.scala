package ru.mipt.acsl.decode.model.component

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.component.message.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.types.{AbstractNameNamespaceInfoAware, StructType}

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

  private class Impl(name: ElementName, namespace: Namespace, var id: Option[Int],
                              var baseTypeProxy: Option[MaybeProxy[StructType]], info: LocalizedString,
                              var subComponents: immutable.Seq[ComponentRef],
                              var commands: immutable.Seq[Command] = immutable.Seq.empty,
                              var eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
                              var statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty)
    extends AbstractNameNamespaceInfoAware(name, namespace, info) with Component

  def apply(name: ElementName, namespace: Namespace, id: Option[Int], baseTypeProxy: Option[MaybeProxy[StructType]],
            info: LocalizedString, subComponents: immutable.Seq[ComponentRef],
            commands: immutable.Seq[Command] = immutable.Seq.empty,
            eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
            statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty): Component =
    new Impl(name, namespace, id, baseTypeProxy, info, subComponents, commands, eventMessages, statusMessages)
}
