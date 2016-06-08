package ru.mipt.acsl.decode.model.component

import java.util
import java.util.Optional

import org.jetbrains.annotations.Nullable
import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.component.message.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.naming.Fqn.newInstance
import ru.mipt.acsl.decode.model.naming.{Fqn, _}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.registry.Language
import ru.mipt.acsl.decode.model.types.StructType
import ru.mipt.acsl.decode.model.types.Alias

import scala.collection.JavaConversions._

/**
  * @author Artem Shein
  */
trait Component extends Container with HasName {

  @Nullable
  def id: Integer

  def namespace: Namespace

  def baseTypeProxy: Option[MaybeProxy.Struct]

  def baseType: Option[StructType] = baseTypeProxy.map(_.obj)

  def namespace_=(ns: Namespace): Unit

  def alias: Alias.NsComponent

  def name: ElementName = alias.name

  def info: util.Map[Language, String] = alias.info

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

  def fqn: Fqn = Fqn.newInstance(namespace.fqn, name)

  def eventMessage(name: ElementName): Option[EventMessage] = objects.flatMap {
    case a: Alias.ComponentEventMessage if a.name == name => Seq(a.obj)
  } match {
    case s if s.size == 1 => Some(s.head)
  }

  def statusMessage(name: ElementName): Option[StatusMessage] = objects.flatMap {
    case a: Alias.ComponentStatusMessage if a.name == name => Seq(a.obj)
    case _ => Seq.empty
  } match {
    case s if s.size == 1 => Some(s.head)
  }

}

object Component {

  private class ComponentImpl(val alias: Alias.NsComponent, var namespace: Namespace, @Nullable var id: Integer,
                              var baseTypeProxy: Option[MaybeProxy.Struct],
                              var objects: util.List[Referenceable] = util.Collections.emptyList())
    extends Component {

    override def objects(objects: util.List[Referenceable]): Unit = this.objects = objects

  }

  def apply(alias: Alias.NsComponent, namespace: Namespace, @Nullable id: Integer,
            baseTypeProxy: Option[MaybeProxy.Struct],
            objects: util.List[Referenceable]): Component =
    new ComponentImpl(alias, namespace, id, baseTypeProxy, objects)
}
