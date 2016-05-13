package ru.mipt.acsl.decode.c.generator

import ru.mipt.acsl.decode.model.component.Component

/**
  * @author Artem Shein
  */

/**
  * Component and additional [[T]] pair
  */
class WithComponent[T](val component: Component, val _2: T)

/**
  * Component and additional value pair matching helper
  */
object WithComponent {
  def apply[T](component: Component, _2: T) = new WithComponent[T](component, _2)

  def unapply[T](o: WithComponent[T]): Option[(Component, T)] = Some(o.component, o._2)
}
