package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.DecodeUtils
import ru.mipt.acsl.decode.model.domain.impl.types.ArrayTypeImpl
import ru.mipt.acsl.decode.model.domain.proxy._
import ru.mipt.acsl.decode.model.domain.proxy.aliases._

/**
  * Created by metadeus on 20.02.16.
  */
class ExistingElementsProxyResolver extends DecodeProxyResolver {
  override def resolveElement(registry: Registry, path: ProxyPath): (Option[Referenceable], ResolvingResult) = {
    val nsOption = DecodeUtils.getNamespaceByFqn(registry, path.ns)
    if (nsOption.isEmpty)
      return (None, Result.error(s"namespace not found ${path.ns}"))
    val ns = nsOption.get
    path.element match {
      case e: TypeName =>
        val name: Option[ElementName] = Some(e.typeName)
        val elements = (ns.units ++ ns.types ++ ns.components).filter(_.optionName.equals(name))
        if (elements.size == 1)
          (Some(elements.head), Result.empty)
        else
          (None, Result.error(s"must be exactly one element for $path, found: ${elements.size}"))
      case e: ArrayTypePath =>
        val arrayTypeName: Option[ElementName] = Some(e.mangledName)
        ns.types.filter(_.optionName.equals(arrayTypeName)) match {
          case s if s.size == 1 =>
            (Some(s.head), Result.empty)
          case s if s.isEmpty =>
            val arrayType = new ArrayTypeImpl(arrayTypeName, ns, None, MaybeProxy.proxy(e.baseTypePath), e.arraySize)
            ns.types = ns.types :+ arrayType
            val result = arrayType.baseType.resolve(registry)
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