package ru.mipt.acsl

import ru.mipt.acsl.ppf.packet.{FixedPacket, Packet}
import ru.mipt.acsl.ppf.property.Element
import ru.mipt.acsl.ppf.types.Varuint

import scalatags.Text.TypedTag
import scalatags.Text.all.{Tag => _, _}

/**
  * Created by metadeus on 04.05.16.
  */
package object ppf {

  implicit class Packet2HtmlHelper(val packet: Packet) {

    def toHtml: Seq[Modifier] = {

      val propsByRank = packet.properties.sortBy(_.rank)

      Seq(table(cls := "table table-bordered",
        thead(tr(th("Название"), th("Тип"), th("Значение"), th("Описание"))),
        tbody(
          Seq(tr(
            td("Заголовок пакета"),
            td(Varuint.toString),
            td(packet.propsAsUint),
            td("Стартовая последовательность пакета"))) ++
            propsByRank.flatMap(_.beforeElements(packet).flatMap(_.toHtml)) ++
            packet.dataInfo.toHtml ++
            propsByRank.reverse.flatMap(_.afterElements(packet).flatMap(_.toHtml)): _*)))
    }

  }

  implicit class FixedPacket2HtmlHelper(val packet: FixedPacket) {

    def toHtml: Modifier = {
      table(cls := "table table-bordered",
        thead(tr(th("Название"), th("Тип"), th("Значение"), th("Описание"))),
        tbody(packet.dataInfo.toHtml: _*))
    }

  }

}
