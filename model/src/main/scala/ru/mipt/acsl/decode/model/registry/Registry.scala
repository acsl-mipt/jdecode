package ru.mipt.acsl.decode.model.registry

import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.proxy._
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.{Message, Referenceable, ReferenceableVisitor}

/**
  * @author Artem Shein
  */
trait Registry extends Referenceable {

  def rootNamespace: Namespace

  def rootNamespace_=(ns: Namespace): Unit

  def proxyResolvers: Seq[DecodeProxyResolver]

  def resolveElement(path: ProxyPath): ResolvingResult[Referenceable] = {
    for (resolver <- proxyResolvers) {
      val resultAndMessages = resolver.resolveElement(this, path)
      if (resultAndMessages.result.isPresent)
        return resultAndMessages
    }
    ResolvingResult.newInstance(Message.newInstance(Level.ERROR, s"path $path can not be resolved"))
  }

  def accept(visitor: ReferenceableVisitor) {
    visitor.visit(this)
  }

}

object Registry {

  private case class RegistryImpl(name: ElementName, var rootNamespace: Namespace, var proxyResolvers: Seq[DecodeProxyResolver])
    extends Registry {

    if (Fqn.DECODE_NAMESPACE.size != 1)
      sys.error("not implemented")

  }

  def apply(): Registry = {
    RegistryImpl(ElementName.newInstanceFromMangledName("GlobalRegistry"), Namespace.newInstanceRoot(),
      Seq(ExistingElementsProxyResolver, NativeLiteralGenericTypesProxyResolver))
  }
}
