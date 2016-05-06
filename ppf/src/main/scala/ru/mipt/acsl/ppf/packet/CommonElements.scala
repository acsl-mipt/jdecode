package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Text}
import ru.mipt.acsl.ppf.property.Element
import ru.mipt.acsl.ppf.types.{U32, Varuint}

/**
  * @author Artem Shein
  */
object CommonElements {

  val ComponentNumber = Element("Номер бортового компонента", Varuint, EmptyHtmlInfo, Text("Определяет номер компонента к которому относится ТМ-сообщение"))
  val TmMessageNumber = Element("Номер ТМ-сообщения", Varuint, EmptyHtmlInfo, Text("Определяет номер ТМ-сообщения в рамках компонента"))
  val EventNumber = Element("Номер события", Varuint, EmptyHtmlInfo, Text("Определяет номер события из ТМ-сообщения"))
  val EventTmMessageTimestamp = Element("Метка времени", U32)

}
