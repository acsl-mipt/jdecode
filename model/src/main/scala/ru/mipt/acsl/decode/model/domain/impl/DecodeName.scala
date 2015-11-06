package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain

/**
  * @author Artem Shein
  */
case class DecodeName(value: String) extends domain.IDecodeName {
  override def asString(): String = value
}

object DecodeName {
  def newFromSourceName(name: String) = DecodeName(domain.IDecodeName.mangleName(name))
  def newFromMangledName(name: String) = DecodeName(name)
}
