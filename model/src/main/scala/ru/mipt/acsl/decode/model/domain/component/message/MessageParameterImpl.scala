package ru.mipt.acsl.decode.model.domain.component.message

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.component.message

/**
  * @author Artem Shein
  */
private[domain] class MessageParameterImpl(val path: message.MessageParameterPath, val info: LocalizedString = LocalizedString.empty)
  extends message.MessageParameter {

  override def toString: String = path.map(_.fold(_.asMangledString.mkString(".", "", ""), _.toString.mkString("[", "", "]"))).mkString.substring(1)

}
