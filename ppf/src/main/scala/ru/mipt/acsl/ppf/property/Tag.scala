package ru.mipt.acsl.ppf.property

import ru.mipt.acsl.ppf.html.Text
import ru.mipt.acsl.ppf.packet.Packet
import ru.mipt.acsl.ppf.types.Varuint

/**
  * @author Artem Shein
  */
class Tag(val value: Int) extends Property {

  val rank = 2

  override def beforeElements(packet: Packet) = Seq(
    Element("Идентификатор пакета", Varuint, Text(value.toString)))

}

object Tag {

  def apply(value: Int): Tag = new Tag(value)

}
