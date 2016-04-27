package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.message

/**
  * @author Artem Shein
  */
object MessageParameter {
  def apply(path: message.MessageParameterPath, info: LocalizedString = LocalizedString.empty): message.MessageParameter =
    new MessageParameterImpl(path, info)
}
