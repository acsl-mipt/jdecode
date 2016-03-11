package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.component.messages.MessageParameter
import ru.mipt.acsl.decode.model.domain.impl.LocalizedString

/**
  * @author Artem Shein
  */
object MessageParameter {
  def apply(value: String, info: LocalizedString = LocalizedString.empty): MessageParameter =
    new MessageParameterImpl(value, info)
}
