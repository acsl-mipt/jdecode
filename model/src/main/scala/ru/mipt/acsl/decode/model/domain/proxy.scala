package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.impl.{DecodeNameImpl, ProxyImpl}
import ru.mipt.acsl.decode.modeling.ErrorLevel
import ru.mipt.acsl.decode.modeling.aliases.ResolvingMessage
import ru.mipt.acsl.decode.modeling.impl.MessageImpl

import scala.collection.immutable

/**
  * @author Artem Shein
  */

case class ResolvingResult[+T <: Referenceable](resolvedObject: Option[T], messages: Seq[ResolvingMessage] = Seq.empty) {
  def hasError: Boolean = messages.contains((m: ResolvingMessage) => m.level == ErrorLevel)
}

object ResolvingResult {
  def error[T <: Referenceable](msg: String): ResolvingResult[T] =
    ResolvingResult[T](None, Seq(MessageImpl(ErrorLevel, msg)))
  def empty[T <: Referenceable]: ResolvingResult[T] = ResolvingResult[T](None)
  def merge[T <: Referenceable](left: ResolvingResult[T], right: ResolvingResult[T]): ResolvingResult[T] =
    ResolvingResult[T](left.resolvedObject.orElse(right.resolvedObject), left.messages ++ right.messages)
}

sealed abstract class ProxyElementName {
  def mangledName: DecodeName
  override def toString: String = mangledName.asMangledString
}
case class PrimitiveTypeName(typeKind: TypeKind.Value, bitSize: Int) extends ProxyElementName {
  override def mangledName: DecodeName =
    DecodeNameImpl.newFromMangledName(TypeKind.nameForTypeKind(typeKind) + ":" + bitSize)
}
case class GenericTypeName(typeName: DecodeName, genericArgumentPaths: immutable.Seq[Option[ProxyPath]])
  extends ProxyElementName {
  override def mangledName: DecodeName = DecodeNameImpl.newFromMangledName(typeName.asMangledString + "<" +
    genericArgumentPaths.map(_.map(_.mangledName).getOrElse(DecodeNameImpl.newFromMangledName("void"))).mkString(",") +
    ">")
}
case class TypeName(typeName: DecodeName) extends ProxyElementName {
  override def mangledName: DecodeName = typeName
}
case class ArrayTypePath(baseTypePath: ProxyPath, arraySize: ArraySize) extends ProxyElementName {
  def mangledName: DecodeName = DecodeNameImpl.newFromMangledName(s"[${baseTypePath.mangledName}]")
}

class ProxyPath(val ns: Fqn, val element: ProxyElementName) {
  def mangledName: DecodeName = DecodeNameImpl.newFromMangledName(s"$ns.${element.mangledName.asMangledString}")
  override def toString: String = s"ProxyPath{${ns.asMangledString}.$element}"
}

object ProxyPath {
  def apply(nsFqn: Fqn, name: DecodeName): ProxyPath = new ProxyPath(nsFqn, TypeName(name))
  def apply(nsFqn: Fqn, element: ProxyElementName): ProxyPath = new ProxyPath(nsFqn, element)
}

trait Proxy[T <: Referenceable] {
  def resolve(registry: Registry, cls: Class[T]): ResolvingResult[T]

  def path: ProxyPath
}

class MaybeProxy[T <: Referenceable](var v: Either[Proxy[T], T]) {

  // TODO: refactoring
  def resolve(registry: Registry, cls: Class[T]): ResolvingResult[T] = {
    if (isResolved)
      return ResolvingResult(Some(obj))
    val resolvingResult = proxy.resolve(registry, cls)
    if (resolvingResult.resolvedObject.isDefined)
    {
      v = Right(resolvingResult.resolvedObject.get)
      return resolvingResult
    }
    ResolvingResult.error(s"Can't resolve proxy '$this'")
  }

  def isProxy: Boolean = v.isLeft

  def isResolved: Boolean = v.isRight

  def obj: T = v.right.getOrElse(sys.error(s"assertion error for $proxy"))

  def proxy: Proxy[T] = v.left.getOrElse(sys.error("assertion error"))

  override def toString: String = s"MaybeProxy{${if (isProxy) proxy else obj}}"
}

object MaybeProxy {
  def proxy[T <: Referenceable](proxy: Proxy[T]): MaybeProxy[T] = new MaybeProxy[T](Left(proxy))

  def proxy[T <: Referenceable](path: ProxyPath): MaybeProxy[T] = proxy(ProxyImpl[T](path))

  def proxy[T <: Referenceable](namespaceFqn: Fqn, name: ProxyElementName): MaybeProxy[T] =
    proxy(ProxyPath(namespaceFqn, name))

  def proxyForSystem[T <: Referenceable](elementName: ProxyElementName): MaybeProxy[T] =
    proxy(DecodeConstants.SYSTEM_NAMESPACE_FQN, elementName)

  def proxyDefaultNamespace[T <: Referenceable](elementFqn: Fqn, defaultNamespace: Namespace): MaybeProxy[T] =
    if (elementFqn.size > 1)
      proxy(elementFqn.copyDropLast(), TypeName(elementFqn.last))
    else
      proxy(defaultNamespace.fqn, TypeName(elementFqn.last))

  def obj[T <: Referenceable](obj: T) = new MaybeProxy[T](Right(obj))
}

trait DecodeProxyResolver {
  def resolve[T <: Referenceable](registry: Registry, path: ProxyPath, cls: Class[T]): ResolvingResult[T]
}