package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.pure.component.{message => m}
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private class StatusMessageImpl(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
                                val parameters: Seq[m.MessageParameter], val priority: Option[Int] = None)
  extends AbstractImmutableMessage(component, name, id, info) with m.StatusMessage
