package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.decode.model.Referenceable
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.registry.Registry

/**
  * @author Artem Shein
  */
trait Proxy {

  def resolveElement(registry: Registry): ResolvingResult[Referenceable]

  def path: ProxyPath

}

object Proxy {

  case class ProxyImpl(path: ProxyPath) extends Proxy {

    override def resolveElement(registry: Registry): ResolvingResult[Referenceable] =
      registry.resolveElement(path)

    override def toString: String = path.toString

  }

  def apply(path: ProxyPath): Proxy = ProxyImpl(path)

}
