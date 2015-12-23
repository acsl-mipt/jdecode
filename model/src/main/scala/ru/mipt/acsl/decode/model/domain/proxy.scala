package ru.mipt.acsl.decode.model.domain

import java.net.URI

import ru.mipt.acsl.decode.modeling.ResolvingResult

/**
  * @author Artem Shein
  */
trait DecodeResolvingResult[+T <: DecodeReferenceable] extends ResolvingResult {
  def resolvedObject: Option[T]
}

trait DecodeProxy[T <: DecodeReferenceable] {
  def resolve(registry: DecodeRegistry, cls: Class[T]): DecodeResolvingResult[T]

  def uri: URI
}

trait DecodeMaybeProxy[T <: DecodeReferenceable] {
  def resolve(registry: DecodeRegistry, cls: Class[T]): DecodeResolvingResult[T]

  def isProxy: Boolean

  def isResolved: Boolean = !isProxy

  def obj: T

  def proxy: DecodeProxy[T]
}

trait DecodeProxyResolver {
  def resolve[T <: DecodeReferenceable](registry: DecodeRegistry, uri: URI, cls: Class[T]): DecodeResolvingResult[T]
}