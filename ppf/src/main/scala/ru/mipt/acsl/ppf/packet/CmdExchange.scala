package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.Html
import ru.mipt.acsl.ppf.property.{Address, Tag, Timestamp}

import scalatags.Text.all._

/**
  * Created by metadeus on 05.05.16.
  */
object CmdExchange extends Packet("Адресный пакет командного обменного потока данных",
  Seq(Tag(94), Address(), Timestamp()),
  Html(p("Формат данных потока описан в разделе", a(href := "#streams-app-cmd", "Командный прикладной поток"))),
  Html(p("Пакет обменного потока данных обеспечивает обмен командными данными, пакет маркируется временем формирования.")))
