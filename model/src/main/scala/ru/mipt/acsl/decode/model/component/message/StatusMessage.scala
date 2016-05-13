package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.component.{Component, message}
import ru.mipt.acsl.decode.model.naming.ElementName

trait StatusMessage extends TmMessage {
  def priority: Option[Int]

  def parameters: Seq[MessageParameter]
}

object StatusMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
            parameters: Seq[MessageParameter], priority: Option[Int] = None): StatusMessage =
    new StatusMessageImpl(component, name, id, info, parameters, priority)
}