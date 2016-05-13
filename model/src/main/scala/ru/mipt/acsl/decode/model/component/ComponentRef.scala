package ru.mipt.acsl.decode.model.component

import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait ComponentRef {

  def alias: Option[String]

  def componentProxy: MaybeProxy[Component]

  def component: Component = componentProxy.obj

}

object ComponentRef {

  private class Impl(val componentProxy: MaybeProxy[Component], val alias: Option[String] = None)
    extends ComponentRef

  def apply(componentProxy: MaybeProxy[Component], alias: Option[String] = None): ComponentRef =
    new Impl(componentProxy, alias)
}
