package ru.mipt.acsl.decode.model.domain.proxy

import ru.mipt.acsl.decode.model.domain.{LocalizedString, Referenceable}
import ru.mipt.acsl.decode.model.domain.impl.types.ArrayType
import ru.mipt.acsl.decode.model.domain.proxy.path.{ArrayTypePath, ProxyPath, TypeName}
import ru.mipt.acsl.decode.model.domain.impl.registry._

/**
  * Created by metadeus on 20.02.16.
  */
class ExistingElementsProxyResolver extends DecodeProxyResolver {
  override def resolveElement(registry: Registry, path: ProxyPath): (Option[Referenceable], ResolvingResult) = {
    val nsOption = registry.findNamespace(path.ns)
    if (nsOption.isEmpty)
      return (None, Result.error(s"namespace not found ${path.ns.asMangledString}"))
    val ns = nsOption.get
    path.element match {
      case e: TypeName =>
        val name = e.typeName
        val elements = (ns.units ++ ns.types ++ ns.components).filter(_.name.equals(name))
        if (elements.size == 1)
          (Some(elements.head), Result.empty)
        else
          (None, Result.error(s"must be exactly one element for $path, found: ${elements.size}"))
      case e: ArrayTypePath =>
        val arrayTypeName = e.mangledName
        ns.types.filter(_.name.equals(arrayTypeName)) match {
          case s if s.size == 1 =>
            (Some(s.head), Result.empty)
          case s if s.isEmpty =>
            val arrayType = ArrayType(arrayTypeName, ns, LocalizedString.empty, MaybeProxy(e.baseTypePath), e.arraySize)
            ns.types = ns.types :+ arrayType
            val result = arrayType.baseTypeProxy.resolve(registry)
            if (result.hasError)
              (None, result)
            else
              (Some(arrayType), Result.empty)
          case s =>
            (None, Result.error(s"must be exactly one element for $path, found: $s"))
        }
      case _ =>
        (None, Result.empty)
    }
  }
}