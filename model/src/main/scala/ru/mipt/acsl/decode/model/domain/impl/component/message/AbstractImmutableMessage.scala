package ru.mipt.acsl.decode.model.domain.impl.component.message

/**
  * @author Artem Shein
  */
private abstract class AbstractImmutableMessage(val component: Component, val name: ElementName, val id: Option[Int],
                                                info: Option[String]) extends AbstractMessage(info) {
  def optionName = Some(name)
}
