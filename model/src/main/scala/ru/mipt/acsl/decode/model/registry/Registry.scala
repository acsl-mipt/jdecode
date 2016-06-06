package ru.mipt.acsl.decode.model.registry

import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.proxy.path.ProxyPath
import ru.mipt.acsl.decode.model.proxy.{DecodeProxyResolver, ExistingElementsProxyResolver, NativeLiteralGenericTypesProxyResolver, ResolvingResult}
import ru.mipt.acsl.decode.model.Referenceable
import ru.mipt.acsl.modeling.{ErrorLevel, Message}

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
      for (obj <- resultAndMessages.result)
        return resultAndMessages
    }
    ResolvingResult(None, Seq(Message(ErrorLevel, s"path $path can not be resolved")))
  }

}

object Registry {

  private case class RegistryImpl(name: ElementName, var rootNamespace: Namespace, var proxyResolvers: Seq[DecodeProxyResolver])
    extends Registry {

    if (Fqn.DecodeNamespace.size != 1)
      sys.error("not implemented")

  }

  def apply(): Registry = {
    RegistryImpl(ElementName.newFromMangledName("GlobalRegistry"), Namespace.newRoot,
      Seq(ExistingElementsProxyResolver, NativeLiteralGenericTypesProxyResolver))
  }
}
