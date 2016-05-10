package ru.mipt.acsl.decode.model.domain.pure.component.message

import ru.mipt.acsl.decode.model.domain.{HasInfo, HasOptionId}
import ru.mipt.acsl.decode.model.domain.pure.component.Component
import ru.mipt.acsl.decode.model.domain.pure.naming.HasName

/**
  * @author Artem Shein
  */
trait TmMessage extends HasInfo with HasName with HasOptionId {
  def component: Component
}