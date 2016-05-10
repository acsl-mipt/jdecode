package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
private case class ElementNameImpl(value: String) extends ElementName {
  override def asMangledString: String = value
}
