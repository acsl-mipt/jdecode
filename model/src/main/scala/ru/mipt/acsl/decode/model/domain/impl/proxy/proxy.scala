package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.proxy.{Proxy, ProxyPath}


/**
  * @author Artem Shein
  */
class ProxyImpl[T <: Referenceable](val path: ProxyPath) extends Proxy {
  override def resolveElement(registry: Registry): (Option[Referenceable], ResolvingResult) =
    registry.resolveElement(path)
  override def toString: String = path.toString
}

object Proxy {
  def apply(path: ProxyPath): Proxy = new ProxyImpl(path)
}