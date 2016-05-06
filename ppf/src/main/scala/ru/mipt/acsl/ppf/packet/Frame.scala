package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.{Html, Text}
import ru.mipt.acsl.ppf.property._
import ru.mipt.acsl.ppf.types.Varuint

import scalatags.Text.all.{Tag => _, _}

/**
  * @author Artem Shein
  */
object Frame extends Packet("Информационный кадр канального уровня",
  Seq(Tag(71), Size, Address(2, Seq(
      Element("Вид адресации", Varuint, Text(2.toString)),
      Element("Физический адрес источника", Varuint),
      Element("Физический адрес приемника", Varuint))),
    ErrorCorrectionCode()),
  Html(p("В случае использования мултиплексирования обменных потоков включает в себя пакеты уровня ",
    a(href := "#streams-multiplexing", "Мультиплексирование обменных потоков"),
    ", иначе состав данных описан в разделе ", a(href := "#streams-exchange", "Обменные потоки данных"), ".")))
