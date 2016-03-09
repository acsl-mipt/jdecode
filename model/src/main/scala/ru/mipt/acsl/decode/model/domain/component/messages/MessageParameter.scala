package ru.mipt.acsl.decode.model.domain.component.messages

import ru.mipt.acsl.decode.model.domain.HasInfo
import ru.mipt.acsl.decode.model.domain.component.Component

trait MessageParameter extends HasInfo {
  def value: String

  def ref(component: Component): MessageParameterRef
}