package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.messages.MessageParameter

/**
  * @author Artem Shein
  */
object MessageParameter {
  def apply(value: String, info: LocalizedString = LocalizedString.empty): MessageParameter =
    new MessageParameterImpl(value, info)
}
