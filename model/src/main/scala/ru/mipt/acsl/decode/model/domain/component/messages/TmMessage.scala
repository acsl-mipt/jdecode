package ru.mipt.acsl.decode.model.domain.component.messages

import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.naming.HasName
import ru.mipt.acsl.decode.model.domain.{HasOptionId, HasInfo}

trait TmMessage extends HasInfo with HasName with HasOptionId {
  def component: Component
}