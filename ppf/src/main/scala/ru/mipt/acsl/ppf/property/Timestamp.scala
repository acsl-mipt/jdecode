package ru.mipt.acsl.ppf.property

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Html}
import ru.mipt.acsl.ppf.packet.Packet
import ru.mipt.acsl.ppf.types.Varuint

import scalatags.Text.all._

/**
  * Created by metadeus on 05.05.16.
  */
class Timestamp(kind: Option[Int]) extends Property {
  override def rank: Int = 7

  override def beforeElements(packet: Packet): Seq[Element] = Seq(
    Element("Тип штампа времени", Varuint, EmptyHtmlInfo,
      Html(table(cls := "table",
        thead(
          tr(th("Значение типа штампа времени"), th("Тип метки времени"), th("Описание типа штампа времени"))),
        tbody(
          tr(td("0"), td(), td("Не регламентирован (определен вышестоящим протоколом)")),
          tr(td("1"), td("u32"), td("Unix timestamp")),
          tr(td("2"), td("u32"),
            td("Младшие 22 бита UNIX timestamp в старших 22 битах и 10 бит миллисекунды в младших 10 битах")))))),
    Element("Значение метки времени"))
}

object Timestamp {
  def apply(): Timestamp = apply(None)
  def apply(kind: Option[Int]): Timestamp = new Timestamp(kind)
}