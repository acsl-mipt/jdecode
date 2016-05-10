package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private abstract class AbstractImmutableMessage(val component: Component, val name: ElementName, val id: Option[Int],
                                                info: LocalizedString) extends AbstractMessage(info) {
  def optionName = Some(name)
}
