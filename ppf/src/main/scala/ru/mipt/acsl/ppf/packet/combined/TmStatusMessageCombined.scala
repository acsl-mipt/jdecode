package ru.mipt.acsl.ppf.packet.combined

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Text}
import ru.mipt.acsl.ppf.packet.{CommonElements, FixedPacket}
import ru.mipt.acsl.ppf.property.{Element, Elements, Segmentation}
import ru.mipt.acsl.ppf.types.{U8, Varuint}

/**
  * @author Artem Shein
  */
object TmStatusMessageCombined extends FixedPacket("Статусное ТМ-сообщение",
  Elements(
    Element("Заголовок статусного ТМ-сообщения", U8, Text(1.toString)),
    Element("Длина данных", Varuint, EmptyHtmlInfo, Text("Длина данных ТМ-сообщения")),
    Segmentation.CurrentSegmentNumberElement,
    Segmentation.MaximumSegmentNumberElement,
    CommonElements.ComponentNumber,
    CommonElements.TmMessageNumber,
    Element("Данные параметров ТМ-сообщения")))
