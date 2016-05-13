package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.decode.model.Referenceable
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.registry.Registry

/**
  * @author Artem Shein
  */
trait DecodeProxyResolver {
  def resolveElement(registry: Registry, path: ProxyPath): (Option[Referenceable], ResolvingResult)
}
