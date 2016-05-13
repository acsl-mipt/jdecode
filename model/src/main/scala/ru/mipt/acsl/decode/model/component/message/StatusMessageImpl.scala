package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.component.{message => m}
import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
private class StatusMessageImpl(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
                                val parameters: Seq[m.MessageParameter], val priority: Option[Int] = None)
  extends AbstractImmutableMessage(component, name, id, info) with m.StatusMessage
