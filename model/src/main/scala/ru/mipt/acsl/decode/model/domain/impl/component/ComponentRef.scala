package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure

/**
  * @author Artem Shein
  */
trait ComponentRef extends pure.component.ComponentRef {
  def componentProxy: MaybeProxy[Component]
  override def component: Component = componentProxy.obj
}

object ComponentRef {
  def apply(componentProxy: MaybeProxy[Component], alias: Option[String] = None): ComponentRef =
    new ComponentRefImpl(componentProxy, alias)
}
