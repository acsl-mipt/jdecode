package ru.mipt.acsl.decode.model.domain.pure.component.messages

import ru.mipt.acsl.decode.model.domain.pure.HasInfo

trait MessageParameter extends HasInfo {
  def value: String
}