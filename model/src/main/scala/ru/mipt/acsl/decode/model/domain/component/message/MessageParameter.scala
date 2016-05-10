package ru.mipt.acsl.decode.model.domain.component.message

import ru.mipt.acsl.decode.model.domain.{LocalizedString, _}
import ru.mipt.acsl.decode.model.domain.component.message

trait MessageParameter extends HasInfo {
  def path: MessageParameterPath
}

object MessageParameter {
  def apply(path: message.MessageParameterPath, info: LocalizedString = LocalizedString.empty): message.MessageParameter =
    new MessageParameterImpl(path, info)
}