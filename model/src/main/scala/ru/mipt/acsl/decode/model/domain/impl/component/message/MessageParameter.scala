package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.messages.{MessageParameter, MessageParameterPath}

/**
  * @author Artem Shein
  */
object MessageParameter {
  def apply(path: MessageParameterPath, info: LocalizedString = LocalizedString.empty): MessageParameter =
    new MessageParameterImpl(path, info)
}
