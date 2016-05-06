package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.{Html, Text}
import ru.mipt.acsl.ppf.property.{Size, Tag}

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
object CommandMessage extends Packet("Командное сообщение", Seq(Tag(101), Size), Text("Одна или несколько команд"),
  Html(p("Пакет служит для передачи командного сообщения.")))
