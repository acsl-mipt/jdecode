package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.pure.{naming => n}

/**
  * @author Artem Shein
  */
private case class ElementNameImpl(value: String) extends n.ElementName {
  override def asMangledString: String = value
}
