package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.types.Alias
import ru.mipt.acsl.decode.model.{LocalizedString, Referenceable, StatusParameter}

trait StatusMessage extends TmMessage {

  def alias: Alias.ComponentStatusMessage

  def priority: Option[Int]

  def parameters: Seq[StatusParameter] = objects.flatMap { case m: StatusParameter => Seq(m) }

  override def name: ElementName = alias.name

  override def info: LocalizedString = alias.info

}

object StatusMessage {

  private case class StatusMessageImpl(alias: Alias.ComponentStatusMessage, component: Component, id: Option[Int],
                          var objects: Seq[Referenceable], priority: Option[Int] = None)
    extends StatusMessage

  def apply(alias: Alias.ComponentStatusMessage, component: Component, id: Option[Int],
            parameters: Seq[StatusParameter], priority: Option[Int] = None): StatusMessage =
    StatusMessageImpl(alias, component, id, parameters, priority)
}