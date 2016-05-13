package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.{HasInfo, HasOptionId}
import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.naming.HasName

/**
  * @author Artem Shein
  */
trait TmMessage extends HasInfo with HasName with HasOptionId {
  def component: Component
}