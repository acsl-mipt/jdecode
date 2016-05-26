package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.decode.model.{LocalizedString, Referenceable}
import ru.mipt.acsl.decode.model.proxy.path.{ArrayTypePath, ProxyPath, TypeName}
import ru.mipt.acsl.decode.model.registry._

/**
  * Created by metadeus on 20.02.16.
  */
class ExistingElementsProxyResolver extends DecodeProxyResolver {

  override def resolveElement(registry: Registry, path: ProxyPath): (Option[Referenceable], ResolvingResult) = {
    path match {
      case e: ProxyPath.FqnElement =>
        resolveElement(registry, e)
      case l: ProxyPath.Literal =>
        sys.error("not implemented")
    }
  }

  private def resolveElement(registry: Registry, fqnElement: ProxyPath.FqnElement): (Option[Referenceable], ResolvingResult) = {
    val nsOption = registry.findNamespace(fqnElement.ns)
    if (nsOption.isEmpty)
      return (None, Result.error(s"namespace not found ${fqnElement.ns.asMangledString}"))
    val ns = nsOption.get
    fqnElement.element match {
      case e: TypeName =>
        val name = e.typeName
        val elements = (ns.measures ++ ns.types ++ ns.components).filter(_.name.equals(name))
        if (elements.size == 1)
          (Some(elements.head), Result.empty)
        else
          (None, Result.error(s"must be exactly one element for $fqnElement, found: ${elements.size}"))
      case e: ArrayTypePath =>
        val arrayTypeName = e.mangledName
        ns.types.filter(_.name.equals(arrayTypeName)) match {
          case s if s.size == 1 =>
            (Some(s.head), Result.empty)
          case s if s.isEmpty =>
            sys.error("not implemented")
            /*val arrayType = ArrayType(arrayTypeName, ns, LocalizedString.empty, MaybeProxy(e.baseTypePath), e.arraySize)
            ns.types = ns.types :+ arrayType
            val result = arrayType.baseTypeProxy.resolve(registry)
            if (result.hasError)
              (None, result)
            else
              (Some(arrayType), Result.empty)*/
          case s =>
            (None, Result.error(s"must be exactly one element for $fqnElement, found: $s"))
        }
      case _ =>
        (None, Result.empty)
    }
  }
}