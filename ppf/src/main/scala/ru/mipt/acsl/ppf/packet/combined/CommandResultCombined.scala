package ru.mipt.acsl.ppf.packet.combined

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Text}
import ru.mipt.acsl.ppf.property.Element

/**
  * @author Artem Shein
  */
object CommandResultCombined extends PacketCombined("Ответ на команду комбинированного уровня",
  Element("Данные пакета", None, EmptyHtmlInfo, Text("Данные результата исполнения команды")))
