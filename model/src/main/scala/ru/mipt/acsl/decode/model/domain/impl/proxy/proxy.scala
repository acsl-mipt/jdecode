package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.DecodeConstants
import ru.mipt.acsl.decode.model.domain.naming.{Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy._
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult

import scala.reflect.ClassTag


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

object MaybeProxy {
  def proxy[T <: Referenceable : ClassTag](proxy: Proxy): MaybeProxy[T] = new MaybeProxy[T](Left(proxy))

  def proxy[T <: Referenceable : ClassTag](path: ProxyPath): MaybeProxy[T] = proxy(impl.proxy.Proxy(path))

  def proxy[T <: Referenceable : ClassTag](namespaceFqn: Fqn, name: ProxyElementName): MaybeProxy[T] =
    proxy(ProxyPath(namespaceFqn, name))

  def proxyForSystem[T <: Referenceable : ClassTag](elementName: ProxyElementName): MaybeProxy[T] =
    proxy(DecodeConstants.SYSTEM_NAMESPACE_FQN, elementName)

  def proxyDefaultNamespace[T <: Referenceable : ClassTag](elementFqn: Fqn, defaultNamespace: Namespace): MaybeProxy[T] =
    if (elementFqn.size > 1)
      proxy(elementFqn.copyDropLast, TypeName(elementFqn.last))
    else
      proxy(defaultNamespace.fqn, TypeName(elementFqn.last))

  def obj[T <: Referenceable : ClassTag](obj: T) = new MaybeProxy[T](Right(obj))
}