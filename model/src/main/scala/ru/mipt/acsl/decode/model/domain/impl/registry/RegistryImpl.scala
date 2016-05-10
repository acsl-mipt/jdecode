package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.impl.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.{DecodeProxyResolver, ExistingElementsProxyResolver, PrimitiveAndGenericTypesProxyResolver}
import ru.mipt.acsl.decode.model.domain.naming.ElementName

import scala.collection.immutable

/**
  * @author Artem Shein
  */
private[decode] class RegistryImpl(val name: ElementName, resolvers: DecodeProxyResolver*) extends Registry {
  if (Fqn.SystemNamespace.size != 1)
    sys.error("not implemented")

  var rootNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty

  val proxyResolvers = resolvers.to[immutable.Seq]

  def this() = this(ElementName.newFromMangledName("GlobalRegistry"),
    new ExistingElementsProxyResolver(),
    new PrimitiveAndGenericTypesProxyResolver())

}
