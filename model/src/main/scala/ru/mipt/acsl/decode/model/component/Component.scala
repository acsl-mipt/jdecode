package ru.mipt.acsl.decode.model.component

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.component.message.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.naming._
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.StructType
import ru.mipt.acsl.decode.model.types.Alias

/**
  * @author Artem Shein
  */
trait Component extends Referenceable with Container with HasName {

  def id: Option[Int]

  def namespace: Namespace

  def baseTypeProxy: Option[MaybeProxy.Struct]

  def baseType: Option[StructType] = baseTypeProxy.map(_.obj)

  def namespace_=(ns: Namespace): Unit

  def alias: Alias.NsComponent

  def name: ElementName = alias.name

  def info: LocalizedString = alias.info

  def statusMessages: Seq[StatusMessage] = objects.flatMap {
    case s: StatusMessage => Seq(s)
    case _ => Seq.empty
  }

  def eventMessages: Seq[EventMessage] = objects.flatMap {
    case e: EventMessage => Seq(e)
    case _ => Seq.empty
  }

  def commands: Seq[Command] = objects.flatMap {
    case c: Command => Seq(c)
    case _ => Seq.empty
  }

  def subComponents: Seq[Alias.ComponentComponent] = objects.flatMap {
    case c: Alias.ComponentComponent => Seq(c)
    case _ => Seq.empty
  }

  def fqn: Fqn = Fqn(namespace.fqn, name)

  def eventMessage(name: ElementName): Option[EventMessage] = objects.flatMap {
    case a: Alias.ComponentEventMessage if a.name == name => Seq(a.obj)
  } match {
    case s if s.size == 1 => Some(s.head)
  }

  def statusMessage(name: ElementName): Option[StatusMessage] = objects.flatMap {
    case a: Alias.ComponentStatusMessage if a.name == name => Seq(a.obj)
  } match {
    case s if s.size == 1 => Some(s.head)
  }

}

object Component {

  private class ComponentImpl(val alias: Alias.NsComponent, var namespace: Namespace, var id: Option[Int],
                              var baseTypeProxy: Option[MaybeProxy.Struct], var objects: Seq[Referenceable] = Seq.empty)
    extends Component

  def apply(alias: Alias.NsComponent, namespace: Namespace, id: Option[Int],
            baseTypeProxy: Option[MaybeProxy.Struct],
            objects: Seq[Referenceable]): Component =
    new ComponentImpl(alias, namespace, id, baseTypeProxy, objects)
}
