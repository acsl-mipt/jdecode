package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.domain.impl.proxy.{DecodeProxyResolver, ResolvingResult}
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.Referenceable
import ru.mipt.acsl.decode.modeling.ErrorLevel
import ru.mipt.acsl.decode.modeling.impl.Message

import scala.collection.immutable
import scala.reflect.ClassTag

/**
  * @author Artem Shein
  */
trait Registry extends pure.registry.Registry {

  override def rootNamespaces: immutable.Seq[Namespace]

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
  def apply(): Registry = new RegistryImpl()
}
