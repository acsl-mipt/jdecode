package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Text}
import ru.mipt.acsl.ppf.property.{Element, Elements}
import ru.mipt.acsl.ppf.types.Varuint

/**
  * @author Artem Shein
  */
object Command extends FixedPacket("Команда", Elements(
    Element("Номер бортового компонента", Varuint, EmptyHtmlInfo, Text("Определяет номер компонента к которому относится сообщение")),
    Element("Номер команды", Varuint, EmptyHtmlInfo, Text("Определяет номер команды в рамках компонента")),
    Element("Данные параметров команды", None, EmptyHtmlInfo, Text("Параметры команды"))))
