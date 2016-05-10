package ru.mipt.acsl.decode.model.domain.proxy

import ru.mipt.acsl.decode.model.domain.Referenceable
import ru.mipt.acsl.decode.model.domain.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry

/**
  * @author Artem Shein
  */
class ProxyImpl[T <: Referenceable](val path: ProxyPath) extends Proxy {
  override def resolveElement(registry: Registry): (Option[Referenceable], ResolvingResult) =
    registry.resolveElement(path)

  override def toString: String = path.toString
}
