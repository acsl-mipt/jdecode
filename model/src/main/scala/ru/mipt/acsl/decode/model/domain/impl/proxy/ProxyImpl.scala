package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain.impl.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry
import ru.mipt.acsl.decode.model.domain.pure.Referenceable

/**
  * @author Artem Shein
  */
class ProxyImpl[T <: Referenceable](val path: ProxyPath) extends Proxy {
  override def resolveElement(registry: Registry): (Option[Referenceable], ResolvingResult) =
    registry.resolveElement(path)

  override def toString: String = path.toString
}
