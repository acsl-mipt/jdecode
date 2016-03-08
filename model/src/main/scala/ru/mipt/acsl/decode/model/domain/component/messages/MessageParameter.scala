package ru.mipt.acsl.decode.model.domain.component.messages

import ru.mipt.acsl.decode.model.domain.HasOptionInfo
import ru.mipt.acsl.decode.model.domain.component.Component

trait MessageParameter extends HasOptionInfo {
  def value: String

  def ref(component: Component): MessageParameterRef
}