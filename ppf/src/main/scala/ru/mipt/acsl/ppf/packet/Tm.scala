package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.Html
import ru.mipt.acsl.ppf.property.{Address, Size, Tag, Timestamp}

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
object Tm extends Packet("Адресный пакет телеметрического обменного потока данных",
  Seq(Tag(93), Size, Address(), Timestamp()),
  Html(p("Формат данных потока описан в разделе ", a(href := "#streams-app-tm", "Телеметрический прикладной поток"))),
  Html(p("Пакет обменного потока данных обеспечивает обмен телеметрическими данными, пакет маркируется временем формирования.")))
