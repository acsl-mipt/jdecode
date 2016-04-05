package ru.mipt.acsl.decode.c.generator

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.pure.component.message.StatusMessage

/**
  * @author Artem Shein
  */

/**
  * Component and status message pair matching helper
  */
object ComponentStatusMessage {
  def apply(component: Component, message: StatusMessage) = WithComponent[StatusMessage](component, message)

  def unapply(o: WithComponent[StatusMessage]) = WithComponent.unapply(o)
}