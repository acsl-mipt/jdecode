package ru.mipt.acsl.ppf.packet.combined

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Html}
import ru.mipt.acsl.ppf.property.Element

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
object TmCombined extends PacketCombined("ТМ-информация комбинированного уровня",
  Element("Данные пакета", None, EmptyHtmlInfo, Html(p("Одно или несколько ТМ-сообщений"))))
