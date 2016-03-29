package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.messages.{MessageParameter, StatusMessage}
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private class StatusMessageImpl(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
                                val parameters: Seq[MessageParameter], val priority: Option[Int] = None)
  extends AbstractImmutableMessage(component, name, id, info) with StatusMessage
