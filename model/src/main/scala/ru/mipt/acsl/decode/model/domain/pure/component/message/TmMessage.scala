package ru.mipt.acsl.decode.model.domain.pure.component.message

import ru.mipt.acsl.decode.model.domain.pure.component.Component
import ru.mipt.acsl.decode.model.domain.pure.naming.HasName
import ru.mipt.acsl.decode.model.domain.pure.{HasInfo, HasOptionId}

/**
  * @author Artem Shein
  */
trait TmMessage extends HasInfo with HasName with HasOptionId {
  def component: Component
}