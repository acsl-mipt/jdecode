package ru.mipt.acsl.decode.model
package component
package message

import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
private abstract class AbstractImmutableMessage(val component: Component, val name: ElementName,
                                                        val id: Option[Int], info: LocalizedString)
  extends AbstractMessage(info) {
  def optionName = Some(name)
}
