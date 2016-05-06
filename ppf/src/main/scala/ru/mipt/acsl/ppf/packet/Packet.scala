package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, HtmlInfo}
import ru.mipt.acsl.ppf.property.Property

/**
  * @author Artem Shein
  */
class Packet(val title: String, val properties: Seq[Property], val dataInfo: HtmlInfo, val packetInfo: HtmlInfo = EmptyHtmlInfo) {

  assert(properties.map(_.rank).toSet.size == properties.size, "Prop duplicate")

  def propsAsUint: Int = properties.map(1 << _.rank).reduce(_ | _)

}
