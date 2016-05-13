package ru.mipt.acsl.decode.c.generator

import ru.mipt.acsl.decode.model.component.{Command, Component}

/**
  * @author Artem Shein
  */
object ComponentCommand {
  def apply(component: Component, command: Command) = WithComponent[Command](component, command)

  def unapply(o: WithComponent[Command]) = WithComponent.unapply(o)
}
