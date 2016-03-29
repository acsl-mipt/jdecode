package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain.impl.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry
import ru.mipt.acsl.decode.model.domain.pure.Referenceable

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
