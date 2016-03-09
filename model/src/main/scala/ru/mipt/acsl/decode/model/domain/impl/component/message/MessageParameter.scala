package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.messages.MessageParameter
import ru.mipt.acsl.decode.model.domain.impl.ElementInfo

/**
  * @author Artem Shein
  */
object MessageParameter {
  def apply(value: String, info: ElementInfo = ElementInfo.empty): MessageParameter =
    new MessageParameterImpl(value, info)
}
