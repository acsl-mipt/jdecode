package ru.mipt.acsl.decode.model.domain
package component
package message

import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
private[domain] abstract class AbstractImmutableMessage(val component: Component, val name: ElementName, val id: Option[Int],
                                                info: LocalizedString) extends AbstractMessage(info) {
  def optionName = Some(name)
}
