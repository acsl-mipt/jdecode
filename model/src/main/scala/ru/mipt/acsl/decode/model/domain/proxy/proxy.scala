package ru.mipt.acsl.decode.model.domain.proxy

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.ElementName
import ru.mipt.acsl.decode.modeling.ErrorLevel
import ru.mipt.acsl.decode.modeling.aliases.ResolvingMessage
import ru.mipt.acsl.decode.modeling.impl.Message

import scala.collection.immutable
import scala.reflect._

/**
  * @author Artem Shein
  */

package object aliases {
  type ResolvingResult = Seq[ResolvingMessage]

  implicit class RichResolvingResult(result: ResolvingResult) {
    def hasError: Boolean = result.exists(_.level == ErrorLevel)
  }
}

import ru.mipt.acsl.decode.model.domain.proxy.aliases._

object ResolvingResult {
  def apply(msgs: ResolvingMessage*): ResolvingResult = msgs
  def error(msg: String): ResolvingResult = ResolvingResult(Message(ErrorLevel, msg))
  def empty: ResolvingResult = ResolvingResult()
}

sealed abstract class ProxyElementName {
  def mangledName: ElementName
  override def toString: String = mangledName.asMangledString
}
case class PrimitiveTypeName(typeKind: TypeKind.Value, bitSize: Int) extends ProxyElementName {
  override def mangledName: ElementName =
    ElementName.newFromMangledName(TypeKind.nameForTypeKind(typeKind) + ":" + bitSize)
}
case class GenericTypeName(typeName: ElementName, genericArgumentPaths: immutable.Seq[Option[ProxyPath]])
  extends ProxyElementName {
  override def mangledName: ElementName = ElementName.newFromMangledName(typeName.asMangledString + "<" +
    genericArgumentPaths.map(_.map(_.mangledName).getOrElse(ElementName.newFromMangledName("void"))).mkString(",") +
    ">")
}
case class TypeName(typeName: ElementName) extends ProxyElementName {
  override def mangledName: ElementName = typeName
}
case class ArrayTypePath(baseTypePath: ProxyPath, arraySize: ArraySize) extends ProxyElementName {
  def mangledName: ElementName = ElementName.newFromMangledName(s"[${baseTypePath.mangledName}]")
}

class ProxyPath(val ns: Fqn, val element: ProxyElementName) {
  def mangledName: ElementName = ElementName.newFromMangledName(s"$ns.${element.mangledName.asMangledString}")
  override def toString: String = s"ProxyPath{${ns.asMangledString}.$element}"
}

object ProxyPath {
  def apply(nsFqn: Fqn, name: ElementName): ProxyPath = new ProxyPath(nsFqn, TypeName(name))
  def apply(nsFqn: Fqn, element: ProxyElementName): ProxyPath = new ProxyPath(nsFqn, element)
}

trait Proxy {
  def resolveElement(registry: Registry): (Option[Referenceable], ResolvingResult)

  def path: ProxyPath
}

class MaybeProxy[T <: Referenceable : ClassTag](var v: Either[Proxy, T]) extends Resolvable {

  def resolve(registry: Registry): ResolvingResult = {
    if (isResolved)
      return ResolvingResult.empty
    val resolvingResult = registry.resolveElement[T](proxy.path)
    resolvingResult._1.foreach { o =>
        v = Right(o)
    }
    resolvingResult._2
  }

  def isProxy: Boolean = v.isLeft

  def isResolved: Boolean = v.isRight

  def obj: T = v.right.getOrElse(sys.error(s"assertion error for $proxy"))

  def proxy: Proxy = v.left.getOrElse(sys.error("assertion error"))

  override def toString: String = s"MaybeProxy{${if (isProxy) proxy else obj}}"
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
      proxy(elementFqn.copyDropLast(), TypeName(elementFqn.last))
    else
      proxy(defaultNamespace.fqn, TypeName(elementFqn.last))

  def obj[T <: Referenceable : ClassTag](obj: T) = new MaybeProxy[T](Right(obj))
}

trait DecodeProxyResolver {
  def resolveElement(registry: Registry, path: ProxyPath): (Option[Referenceable], ResolvingResult)
}