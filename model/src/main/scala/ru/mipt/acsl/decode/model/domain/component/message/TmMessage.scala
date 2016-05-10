package ru.mipt.acsl.decode.model.domain.component.message

import ru.mipt.acsl.decode.model.domain.{HasInfo, HasOptionId}
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.naming.HasName

/**
  * @author Artem Shein
  */
trait TmMessage extends HasInfo with HasName with HasOptionId {
  def component: Component
}