package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.{message => m}
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */

object StatusMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
            parameters: Seq[m.MessageParameter], priority: Option[Int] = None): m.StatusMessage =
    new StatusMessageImpl(component, name, id, info, parameters, priority)
}
