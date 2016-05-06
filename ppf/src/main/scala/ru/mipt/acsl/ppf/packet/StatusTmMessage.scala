package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Html}
import ru.mipt.acsl.ppf.property.{Segmentation, Size, Tag}

import scalatags.Text.all.{Tag => _, _}

/**
  * @author Artem Shein
  */
object StatusTmMessage extends Packet("Статусное ТМ-сообщение", Seq(Tag(114), Size, Segmentation), EmptyHtmlInfo,
  Html(p("Пакет содержит данные статусного ТМ-сообщения.")))
