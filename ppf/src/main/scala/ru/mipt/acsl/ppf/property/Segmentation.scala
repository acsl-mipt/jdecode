package ru.mipt.acsl.ppf.property

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Text}
import ru.mipt.acsl.ppf.packet.Packet
import ru.mipt.acsl.ppf.types.Varuint

/**
  * @author Artem Shein
  */
object Segmentation extends Property {

  val CurrentSegmentNumberElement = Element("Номер текущего сегмента", Varuint, EmptyHtmlInfo,
    Text("В случае несегментированного пакета должен быть установлен в 0."))

  val MaximumSegmentNumberElement = Element("Максимальный номер сегмента", Varuint, EmptyHtmlInfo,
    Text("В случае несегментированного пакета должен быть установлен в 0."))

  val rank = 4

  override def beforeElements(packet: Packet): Seq[Element] = Seq(
    CurrentSegmentNumberElement,
    MaximumSegmentNumberElement)
}
