package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.Html
import ru.mipt.acsl.ppf.property.{Element, Elements, Size, Tag}

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
object CommandResult extends Packet("Результат исполнения команды", Seq(Tag(102), Size), Elements(
  Element("Данные результата исполнения команды")), Html(p("Пакет содержит данные результата исполнения команды.")))
