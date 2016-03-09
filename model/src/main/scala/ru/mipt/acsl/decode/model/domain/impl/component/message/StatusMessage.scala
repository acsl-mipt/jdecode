package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.component.messages.{MessageParameter, StatusMessage}
import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
object StatusMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: ElementInfo,
            parameters: Seq[MessageParameter], priority: Option[Int] = None): StatusMessage =
    new StatusMessageImpl(component, name, id, info, parameters, priority)
}
