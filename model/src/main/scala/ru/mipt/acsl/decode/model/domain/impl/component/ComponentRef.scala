package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.impl.ComponentRefImpl

/**
  * @author Artem Shein
  */
object ComponentRef {
  def apply(component: MaybeProxy[Component], alias: Option[String] = None): ComponentRef =
    new ComponentRefImpl(component, alias)
}
