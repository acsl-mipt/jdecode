package ru.mipt.acsl.decode.model.domain.proxy

import ru.mipt.acsl.decode.model.domain.Referenceable
import ru.mipt.acsl.decode.model.domain.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry

/**
  * @author Artem Shein
  */
trait DecodeProxyResolver {
  def resolveElement(registry: Registry, path: ProxyPath): (Option[Referenceable], ResolvingResult)
}
