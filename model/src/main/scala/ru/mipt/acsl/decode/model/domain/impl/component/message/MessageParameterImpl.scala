package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.messages.{MessageParameter, MessageParameterPath}

/**
  * @author Artem Shein
  */
private class MessageParameterImpl(val path: MessageParameterPath, val info: LocalizedString = LocalizedString.empty)
  extends MessageParameter {

  override def toString: String = path.map(_.fold(_.asMangledString, _.toString)).mkString.substring(1)

}
