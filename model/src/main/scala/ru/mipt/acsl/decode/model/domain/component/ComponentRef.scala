package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait ComponentRef {

  def alias: Option[String]

  def componentProxy: MaybeProxy[Component]

  def component: Component = componentProxy.obj

}

object ComponentRef {
  def apply(componentProxy: MaybeProxy[Component], alias: Option[String] = None): ComponentRef =
    new ComponentRefImpl(componentProxy, alias)
}
