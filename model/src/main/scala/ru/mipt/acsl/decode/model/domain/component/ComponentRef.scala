package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy

/**
  * Created by rexer on 08.03.16.
  */
trait ComponentRef {
  def component: MaybeProxy[Component]

  def alias: Option[String]

  def aliasOrMangledName: String = alias.getOrElse(component.obj.name.asMangledString)
}
