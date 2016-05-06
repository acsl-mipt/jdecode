package ru.mipt.acsl.ppf.packet.combined

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Text}
import ru.mipt.acsl.ppf.packet.Packet
import ru.mipt.acsl.ppf.property._
import ru.mipt.acsl.ppf.types.{U16, U32, Varuint}

/**
  * @author Artem Shein
  */
class PacketCombined(title: String, dataElement: Element) extends Packet(title, Seq(Tag(81), Size, Segmentation),
  Elements(Element("Адрес источника", Varuint), Element("Адрес приемника", Varuint),
    Element("Метка времени формирования пакета", U32, EmptyHtmlInfo,
      Text("Младшие 22 бита UNIX timestamp в старших 22 битах и 10 бит миллисекунды в младших 10 битах")),
    dataElement, Element("Значение контрольной суммы", U16, EmptyHtmlInfo, Text("CRC-16"))))
