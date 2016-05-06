package ru.mipt.acsl.ppf.packet

import ru.mipt.acsl.ppf.html.EmptyHtmlInfo
import ru.mipt.acsl.ppf.property.Tag

/**
  * Created by metadeus on 05.05.16.
  */
object Delimiter extends Packet("Разделитель командного обменного потока",
  Seq(Tag(126)), EmptyHtmlInfo)
