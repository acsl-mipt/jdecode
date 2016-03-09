package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.StatusMessageImpl

/**
  * @author Artem Shein
  */
object StatusMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: Option[String],
            parameters: Seq[MessageParameter], priority: Option[Int] = None): StatusMessage =
    new StatusMessageImpl(component, name, id, info, parameters, priority)
}
