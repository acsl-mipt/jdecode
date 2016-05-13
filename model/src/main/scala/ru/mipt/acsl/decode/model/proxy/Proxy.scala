package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.decode.model.Referenceable
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.registry.Registry

/**
  * @author Artem Shein
  */
trait Proxy {
  def resolveElement(registry: Registry): (Option[Referenceable], ResolvingResult)

  def path: ProxyPath
}

object Proxy {

  class Impl[T <: Referenceable](val path: ProxyPath) extends Proxy {
    override def resolveElement(registry: Registry): (Option[Referenceable], ResolvingResult) =
      registry.resolveElement(path)

    override def toString: String = path.toString
  }

  def apply(path: ProxyPath): Proxy = new Impl(path)
}
