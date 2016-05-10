package ru.mipt.acsl.decode.c.generator

import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.component.message.EventMessage

/**
  * @author Artem Shein
  */

/**
  * Component and event message pair matching helper
  */
object ComponentEventMessage {
  def apply(component: Component, message: EventMessage) = WithComponent[EventMessage](component, message)

  def unapply(o: WithComponent[EventMessage]) = WithComponent.unapply(o)
}