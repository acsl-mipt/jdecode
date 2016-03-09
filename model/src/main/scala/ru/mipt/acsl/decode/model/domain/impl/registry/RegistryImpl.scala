package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.impl.DecodeConstants
import ru.mipt.acsl.decode.model.domain.impl.naming.ElementName

/**
  * @author Artem Shein
  */
private class RegistryImpl(val name: ElementName, resolvers: DecodeProxyResolver*) extends Registry {
  if (DecodeConstants.SYSTEM_NAMESPACE_FQN.size != 1)
    sys.error("not implemented")

  var rootNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty

  private val proxyResolvers = resolvers.to[immutable.Seq]

  def this() = this(ElementName.newFromMangledName("GlobalRegistry"),
    new ExistingElementsProxyResolver(),
    new PrimitiveAndGenericTypesProxyResolver())

  override def component(fqn: String): Option[Component] = {
    val dotPos = fqn.lastIndexOf('.')
    val namespaceOptional = namespace(fqn.substring(0, dotPos))
    if (namespaceOptional.isEmpty)
    {
      return None
    }
    val componentName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    namespaceOptional.get.components.find(_.name == componentName)
  }

  override def namespace(fqn: String): Option[Namespace] = {
    var currentNamespaces: Option[Seq[Namespace]] = Some(rootNamespaces)
    var currentNamespace: Option[Namespace] = None
    "\\.".r.split(fqn).foreach(nsName => {
      if (currentNamespaces.isEmpty)
      {
        return None
      }
      val decodeName = ElementName.newFromMangledName(nsName)
      currentNamespace = currentNamespaces.get.find(_.name == decodeName)
      if (currentNamespace.isDefined)
      {
        currentNamespaces = Some(currentNamespace.get.subNamespaces)
      }
      else
      {
        currentNamespaces = None
      }
    })
    currentNamespace
  }

  override def eventMessage(fqn: String): Option[EventMessage] = {
    val dotPos = fqn.lastIndexOf('.')
    val decodeName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    component(fqn.substring(0, dotPos)).map(_.eventMessages.find(_.name == decodeName).orNull)
  }

  override def resolveElement[T <: Referenceable](path: ProxyPath)(implicit ct: ClassTag[T]): (Option[T], ResolvingResult) = {
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

  override def statusMessage(fqn: String): Option[StatusMessage] = {
    val dotPos = fqn.lastIndexOf('.')
    val decodeName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    component(fqn.substring(0, dotPos)).map(_.statusMessages.find(_.name == decodeName).orNull)
  }

  def resolve(): ResolvingResult = resolve(this)

  override def resolve(registry: Registry): ResolvingResult =
    registry.rootNamespaces.flatMap(_.resolve(registry))

  override def validate(registry: Registry): ValidatingResult =
    registry.rootNamespaces.flatMap(_.validate(registry))
}
