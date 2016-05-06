package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.Html
import ru.mipt.acsl.ppf.property._

import scalatags.Text.all.{Tag => _, _}

/**
  * @author Artem Shein
  */
object EventTmMessage extends Packet("Событийное ТМ-сообщение", Seq(Tag(120), Size),
  Elements(
    CommonElements.ComponentNumber,
    CommonElements.TmMessageNumber,
    CommonElements.EventNumber,
    CommonElements.EventTmMessageTimestamp),
  Html(p("Пакет содержит данные событийного ТМ-сообщения.")))