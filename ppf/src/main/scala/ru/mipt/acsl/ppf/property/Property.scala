package ru.mipt.acsl.ppf.property

import ru.mipt.acsl.ppf.packet.Packet

/**
  * @author Artem Shein
  */
trait Property {
  def rank: Int
  def beforeElements(packet: Packet): Seq[Element]
  def afterElements(packet: Packet): Seq[Element] = Seq.empty
}
