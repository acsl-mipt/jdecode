package ru.mipt.acsl.ppf.packet.combined

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Html}
import ru.mipt.acsl.ppf.property._

import scalatags.Text.all.{Tag => _, _}

/**
  * @author Artem Shein
  */
object CommandCombined extends PacketCombined("Командный пакет комбинированного уровня",
  Element("Данные пакета", None, EmptyHtmlInfo,
    Html(p("Одна или несколько ", a(href := "streams-app-cmd-packet", "команд")))))
