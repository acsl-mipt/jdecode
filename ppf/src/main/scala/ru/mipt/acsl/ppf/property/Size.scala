package ru.mipt.acsl.ppf.property

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Html}
import ru.mipt.acsl.ppf.packet.Packet
import ru.mipt.acsl.ppf.types.Varuint

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
case object Size extends Property {

  val rank = 3

  def beforeElements(packet: Packet) = packet.dataInfo match {
    case Elements(els @ _*) =>
      Seq(Element("Длина данных", Varuint, EmptyHtmlInfo,
        Html(p(raw(s"Длина данных пакета включая поля: ${els.map("&laquo;" + _.title + "&raquo;").mkString(", ")}.")))))
    case _ =>
      Seq(Element("Длина данных", Varuint, EmptyHtmlInfo, Html(p(raw(s"Длина данных поля &laquo;Данные пакета&raquo;")))))
  }

}
