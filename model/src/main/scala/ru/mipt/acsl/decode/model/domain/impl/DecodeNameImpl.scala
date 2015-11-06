package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain

/**
  * @author Artem Shein
  */
case class DecodeNameImpl(value: String) extends domain.DecodeName {
  override def asString(): String = value
}

object DecodeNameImpl {
  def newFromSourceName(name: String) = DecodeNameImpl(domain.DecodeName.mangleName(name))
  def newFromMangledName(name: String) = DecodeNameImpl(name)
}
