package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain.impl.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry
import ru.mipt.acsl.decode.model.domain.pure.Referenceable

/**
  * @author Artem Shein
  */
trait DecodeProxyResolver {
  def resolveElement(registry: Registry, path: ProxyPath): (Option[Referenceable], ResolvingResult)
}
