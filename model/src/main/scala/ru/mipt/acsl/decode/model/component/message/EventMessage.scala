package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.{Alias, DecodeType}
import ru.mipt.acsl.decode.model.{LocalizedString, Referenceable, TmParameter}

trait EventMessage extends TmMessage {

  def alias: Alias.ComponentEventMessage

  def baseTypeProxy: MaybeProxy.TypeProxy

  def baseType: DecodeType = baseTypeProxy.obj

  override def name: ElementName = alias.name

  override def info: LocalizedString = alias.info

  override def component: Component = alias.parent

  def parameters: Seq[TmParameter] = objects.flatMap {
    case m: TmParameter => Seq(m)
    case _ => Seq.empty
  }

}

object EventMessage {

  private case class EventMessageImpl(alias: Alias.ComponentEventMessage, id: Option[Int],
                          var objects: Seq[Referenceable],
                          baseTypeProxy: MaybeProxy.TypeProxy)
    extends EventMessage

  def apply(alias: Alias.ComponentEventMessage, id: Option[Int],
            objects: Seq[Referenceable],
            baseTypeProxy: MaybeProxy.TypeProxy): EventMessage =
    EventMessageImpl(alias, id, objects, baseTypeProxy)
}
