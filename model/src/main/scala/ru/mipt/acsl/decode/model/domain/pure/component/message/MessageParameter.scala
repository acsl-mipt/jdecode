package ru.mipt.acsl.decode.model.domain.pure.component.message

import ru.mipt.acsl.decode.model.domain.pure.HasInfo

trait MessageParameter extends HasInfo {
  def path: MessageParameterPath
}