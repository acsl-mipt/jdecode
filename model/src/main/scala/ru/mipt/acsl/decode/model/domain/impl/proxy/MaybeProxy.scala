package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain.impl.naming.{Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.impl.proxy.path.{ProxyElementName, ProxyPath, TypeName}
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry
import ru.mipt.acsl.decode.model.domain.pure.Referenceable
import ru.mipt.acsl.decode.model.domain.pure.naming.Fqn

import scala.reflect.ClassTag

/**
  * @author Artem Shein
  */
class MaybeProxy[T <: Referenceable : ClassTag](var v: Either[Proxy, T]) {

  def resolve(registry: Registry): ResolvingResult = {
    isResolved match {
      case true => Result.empty
      case _ =>
        val resolvingResult = registry.resolveElement[T](proxy.path)
        resolvingResult._1.foreach(o => v = Right(o))
        resolvingResult._2
    }
  }

  def isProxy: Boolean = v.isLeft

  def isResolved: Boolean = v.isRight

  def obj: T = v.right.getOrElse(sys.error(s"assertion error for $proxy"))

  def proxy: Proxy = v.left.getOrElse(sys.error("assertion error"))

  override def toString: String = s"MaybeProxy{${if (isProxy) proxy else obj}}"
}

object MaybeProxy {
  def apply[T <: Referenceable : ClassTag](proxy: Proxy): MaybeProxy[T] = new MaybeProxy[T](Left(proxy))

  def apply[T <: Referenceable : ClassTag](path: ProxyPath): MaybeProxy[T] = apply(Proxy(path))

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
