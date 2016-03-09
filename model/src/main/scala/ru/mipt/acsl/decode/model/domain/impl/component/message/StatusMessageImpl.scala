package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.component.messages.{MessageParameter, StatusMessage}
import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
private class StatusMessageImpl(component: Component, name: ElementName, id: Option[Int], info: ElementInfo,
                                val parameters: Seq[MessageParameter], val priority: Option[Int] = None)
  extends AbstractImmutableMessage(component, name, id, info) with StatusMessage
