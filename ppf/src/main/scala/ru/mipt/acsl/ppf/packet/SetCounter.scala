package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.Html
import ru.mipt.acsl.ppf.property._
import ru.mipt.acsl.ppf.types.Varuint

import scalatags.Text.all.{Tag => _, _}

/**
  * @author Artem Shein
  */
object SetCounter extends Packet("Пакет согласования счетчика", Seq(Tag(91), Size, ErrorCorrectionCode()),
  Elements(Element("Счетчик пакета", Varuint, Html(raw("0&ndash;67823")), Html(p("Новое значение для счетчика пакетов приемника")))),
  Html(p("Пакет согласования счетчика пакетов предназначен для установки счетчика пакетов приемника в необходимое значение с целью восстановления обмена при потере пакета и невозможности его восстановления и пересылки.")))
