package ru.mipt.acsl.decode.model.domain

import java.net.URI

import ru.mipt.acsl.decode.model.domain.impl.{DecodeUtils, DecodeProxyImpl}
import ru.mipt.acsl.decode.modeling.ErrorLevel
import ru.mipt.acsl.decode.modeling.aliases.ResolvingMessage
import ru.mipt.acsl.decode.modeling.impl.MessageImpl

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

trait DecodeProxy[T <: Referenceable] {
  def resolve(registry: Registry, cls: Class[T]): ResolvingResult[T]

  def uri: URI
}

class MaybeProxy[T <: Referenceable](var v: Either[DecodeProxy[T], T]) {

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

  def obj: T = v.right.getOrElse(sys.error("assertion error"))

  def proxy: DecodeProxy[T] = v.left.getOrElse(sys.error("assertion error"))
}

object MaybeProxy {
  def proxy[T <: Referenceable](proxy: DecodeProxy[T]): MaybeProxy[T] = new MaybeProxy[T](Left(proxy))

  def proxy[T <: Referenceable](uri: URI): MaybeProxy[T] = proxy(DecodeProxyImpl[T](uri))

  def proxy[T <: Referenceable](namespaceFqn: DecodeFqn, name: DecodeName): MaybeProxy[T] =
    proxy(DecodeUtils.getUriForNamespaceAndName(namespaceFqn, name))

  def proxyForTypeString[T <: Referenceable](string: String, defaultNamespaceFqn: DecodeFqn): MaybeProxy[T] =
    proxy(DecodeUtils.getUriForSourceTypeFqnString(string, defaultNamespaceFqn))

  def proxyForSystemTypeString[T <: Referenceable](typeString: String): MaybeProxy[T] =
    proxyForTypeString(DecodeUtils.normalizeSourceTypeString(typeString), DecodeConstants.SYSTEM_NAMESPACE_FQN)

  def proxyForSystem[T <: Referenceable](decodeName: DecodeName): MaybeProxy[T] =
    proxy(DecodeConstants.SYSTEM_NAMESPACE_FQN, decodeName)

  def proxyDefaultNamespace[T <: Referenceable](elementFqn: DecodeFqn, defaultNamespace: Namespace): MaybeProxy[T] =
    if (elementFqn.size > 1)
      proxy(elementFqn.copyDropLast(), elementFqn.last)
    else
      proxy(defaultNamespace.fqn, elementFqn.last)

  def obj[T <: Referenceable](obj: T) = new MaybeProxy[T](Right(obj))

  def proxyForTypeUriString[T <: Referenceable](typeUriString: String, defaultNsFqn: DecodeFqn) =
    new MaybeProxy(Left(DecodeProxyImpl.newInstanceFromTypeUriString[T](typeUriString, defaultNsFqn)))
}

trait DecodeProxyResolver {
  def resolve[T <: Referenceable](registry: Registry, uri: URI, cls: Class[T]): ResolvingResult[T]
}