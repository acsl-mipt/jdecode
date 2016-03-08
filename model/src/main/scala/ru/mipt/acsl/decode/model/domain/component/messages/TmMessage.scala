package ru.mipt.acsl.decode.model.domain.component.messages

import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.naming.HasName
import ru.mipt.acsl.decode.model.domain.{HasOptionId, HasOptionInfo}

trait TmMessage extends HasOptionInfo with HasName with HasOptionId {
  def component: Component
}