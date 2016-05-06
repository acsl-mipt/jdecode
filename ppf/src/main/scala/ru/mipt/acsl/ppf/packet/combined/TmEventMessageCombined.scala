package ru.mipt.acsl.ppf.packet.combined

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Text}
import ru.mipt.acsl.ppf.packet.{CommonElements, FixedPacket}
import ru.mipt.acsl.ppf.property.{Element, Elements}
import ru.mipt.acsl.ppf.types.{U8, Varuint}

/**
  * @author Artem Shein
  */
object TmEventMessageCombined extends FixedPacket("Событийне ТМ-сообщение",
  Elements(
    Element("Заголовок событийного ТМ-сообщения", U8, Text(0.toString)),
    Element("Длина данных", Varuint, EmptyHtmlInfo, Text("Длина данных ТМ-сообщения")),
    CommonElements.ComponentNumber,
    CommonElements.TmMessageNumber,
    CommonElements.EventNumber,
    Element("Данные параметров ТМ-сообщения")))
