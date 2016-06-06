package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.decode.model.Referenceable
import ru.mipt.acsl.decode.model.naming.Fqn
import ru.mipt.acsl.decode.model.proxy.path.{ProxyPath, TypeName}
import ru.mipt.acsl.decode.model.registry._

/**
  * Created by metadeus on 20.02.16.
  */
case object ExistingElementsProxyResolver extends DecodeProxyResolver {

  override def resolveElement(registry: Registry, path: ProxyPath): ResolvingResult[Referenceable] = {
    path match {
      case e: ProxyPath.FqnElement =>
        resolveFqnElement(registry, e)
      case l: ProxyPath.Literal =>
        resolveLiteralElement(registry, l)
      case _ => sys.error("not implemented")
    }
  }

  private def resolveLiteralElement(registry: Registry, literal: ProxyPath.Literal): ResolvingResult[Referenceable] = {
    val ns = registry.findNamespace(Fqn.DecodeNamespace).getOrElse(sys.error("decode namespace not found"))
    val elements = ns.aliases.filter(_.name == literal.mangledName)
    if (elements.size == 1) {
      val obj = elements.head.obj
      registry.resolve(obj)
      ResolvingResult(Some(obj))
    }
    else if (elements.size > 1)
      ResolvingResult(None, Result.error(s"must be exactly one element for $literal, found: ${elements.size}"))
    else
      ResolvingResult(None)
  }

  private def resolveFqnElement(registry: Registry, fqnElement: ProxyPath.FqnElement): ResolvingResult[Referenceable] = {
    val nsOption = registry.findNamespace(fqnElement.ns)
    if (nsOption.isEmpty)
      return ResolvingResult(None, Result.error(s"namespace not found ${fqnElement.ns.asMangledString}"))
    val ns = nsOption.get
    fqnElement.element match {
      case e: TypeName =>
        val name = e.typeName
        val elements = ns.aliases.filter(_.name == name)
        if (elements.size == 1) {
          val obj = elements.head.obj
          registry.resolve(obj)
          ResolvingResult(Some(obj))
        }
        else
          ResolvingResult(None, Result.error(s"must be exactly one element for $fqnElement, found: ${elements.size}"))
      case _ =>
        ResolvingResult(None)
    }
  }
}