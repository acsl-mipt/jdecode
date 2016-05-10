package ru.mipt.acsl.decode.model.domain.pure.component.message

import ru.mipt.acsl.decode.model.domain.HasInfo

trait MessageParameter extends HasInfo {
  def path: MessageParameterPath
}