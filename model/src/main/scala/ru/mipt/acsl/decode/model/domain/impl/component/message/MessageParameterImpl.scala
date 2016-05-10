package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.message

/**
  * @author Artem Shein
  */
private class MessageParameterImpl(val path: message.MessageParameterPath, val info: LocalizedString = LocalizedString.empty)
  extends message.MessageParameter {

  override def toString: String = path.map(_.fold(_.asMangledString.mkString(".", "", ""), _.toString.mkString("[", "", "]"))).mkString.substring(1)

}
