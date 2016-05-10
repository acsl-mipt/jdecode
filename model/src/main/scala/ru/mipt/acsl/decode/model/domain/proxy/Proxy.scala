package ru.mipt.acsl.decode.model.domain.proxy

import ru.mipt.acsl.decode.model.domain.Referenceable
import ru.mipt.acsl.decode.model.domain.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry

/**
  * @author Artem Shein
  */
trait Proxy {
  def resolveElement(registry: Registry): (Option[Referenceable], ResolvingResult)

  def path: ProxyPath
}

object Proxy {
  def apply(path: ProxyPath): Proxy = new ProxyImpl(path)
}
