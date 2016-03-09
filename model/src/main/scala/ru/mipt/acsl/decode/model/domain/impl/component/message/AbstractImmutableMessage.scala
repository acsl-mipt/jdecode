package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
private abstract class AbstractImmutableMessage(val component: Component, val name: ElementName, val id: Option[Int],
                                                info: ElementInfo) extends AbstractMessage(info) {
  def optionName = Some(name)
}
