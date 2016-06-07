package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.{HasInfo, MayHaveId, Referenceable, ReferenceableVisitor}
import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.naming.{Container, HasName}

/**
  * @author Artem Shein
  */
trait TmMessage extends MayHaveId with HasName with HasInfo with Container {

  def component: Component

}