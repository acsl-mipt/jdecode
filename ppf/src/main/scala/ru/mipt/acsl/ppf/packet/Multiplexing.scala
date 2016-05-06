package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Html, Text}
import ru.mipt.acsl.ppf.property.{Element, Elements, Size, Tag}
import ru.mipt.acsl.ppf.types.Varuint

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
object Multiplexing extends Packet("Пакет обменного потока данных", Seq(Tag(90), Size),
  Elements(Element("Номер потока данных", Varuint, EmptyHtmlInfo,
    Html(ul(li(raw("1 &mdash; Поток команд или ответов на команды")), li(raw("2 &mdash; Поток ТМ-ифнормации"))))),
    Element("Данные пакета", None, EmptyHtmlInfo,
      Html(p("Формат данных пакета описан в разделе ", a(href := "#streams-exchange", "Обменные потоки данных"))))),
  Text("Пакет обменного потока данных предназначен непосредственно для передачи данных обменного уровня."))