package ru.mipt.acsl.decode.model.registry

import ru.mipt.acsl.decode.model.Referenceable
import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.proxy.{DecodeProxyResolver, ExistingElementsProxyResolver, PrimitiveAndGenericTypesProxyResolver, ResolvingResult}
import ru.mipt.acsl.modeling.{ErrorLevel, Message}

import scala.collection.immutable
import scala.reflect.ClassTag

/**
  * @author Artem Shein
  */
trait Registry extends Referenceable {

  def rootNamespaces: immutable.Seq[Namespace]

  def rootNamespaces_=(ns: immutable.Seq[Namespace]): Unit

  def proxyResolvers: immutable.Seq[DecodeProxyResolver]

  def resolveElement[T <: Referenceable](path: ProxyPath)(implicit ct: ClassTag[T]): (Option[T], ResolvingResult) = {
    for (resolver <- proxyResolvers) {
      val result = resolver.resolveElement(this, path)
      for (obj <- result._1)
        return obj match {
          case ct(o) =>
            (Some(o), result._2)
          case o =>
            (None, result._2 :+ Message(ErrorLevel, s"invalid type ${o.getClass}, expected ${ct.runtimeClass}"))
        }
    }
    (None, Seq(Message(ErrorLevel, s"path $path can not be resolved")))
  }
}

object Registry {

  private class Impl(val name: ElementName, resolvers: DecodeProxyResolver*) extends Registry {
    if (Fqn.SystemNamespace.size != 1)
      sys.error("not implemented")

    var rootNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty

    val proxyResolvers = resolvers.to[immutable.Seq]

    def this() = this(ElementName.newFromMangledName("GlobalRegistry"),
      new ExistingElementsProxyResolver(),
      new PrimitiveAndGenericTypesProxyResolver())

  }

  def apply(): Registry = new Impl()
}
