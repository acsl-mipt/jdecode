package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.naming.Fqn
import ru.mipt.acsl.decode.model.domain.naming.{Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy._
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.registry.Registry

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
  def apply[T <: Referenceable : ClassTag](proxy: Proxy): MaybeProxy[T] = new MaybeProxy[T](Left(proxy))

  def apply[T <: Referenceable : ClassTag](path: ProxyPath): MaybeProxy[T] = apply(impl.proxy.Proxy(path))

  def apply[T <: Referenceable : ClassTag](namespaceFqn: Fqn, name: ProxyElementName): MaybeProxy[T] =
    apply(ProxyPath(namespaceFqn, name))

  def apply[T <: Referenceable : ClassTag](obj: T) = new MaybeProxy[T](Right(obj))

  def proxyForSystem[T <: Referenceable : ClassTag](elementName: ProxyElementName): MaybeProxy[T] =
    apply(Fqn.SystemNamespace, elementName)

  def proxyDefaultNamespace[T <: Referenceable : ClassTag](elementFqn: Fqn, defaultNamespace: Namespace): MaybeProxy[T] =
    if (elementFqn.size > 1)
      apply(elementFqn.copyDropLast, TypeName(elementFqn.last))
    else
      apply(defaultNamespace.fqn, TypeName(elementFqn.last))
}