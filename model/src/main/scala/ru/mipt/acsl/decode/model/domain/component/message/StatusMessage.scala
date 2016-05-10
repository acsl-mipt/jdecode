package ru.mipt.acsl.decode.model.domain.component.message

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.component.{Component, message}
import ru.mipt.acsl.decode.model.domain.naming.ElementName

trait StatusMessage extends TmMessage {
  def priority: Option[Int]

  def parameters: Seq[MessageParameter]
}

object StatusMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
            parameters: Seq[MessageParameter], priority: Option[Int] = None): StatusMessage =
    new StatusMessageImpl(component, name, id, info, parameters, priority)
}