package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.DecodeUtils
import ru.mipt.acsl.decode.model.domain.impl.types.ArrayTypeImpl

/**
  * Created by metadeus on 20.02.16.
  */
class FindExistingDecodeProxyResolver extends DecodeProxyResolver {
  override def resolve[T <: Referenceable](registry: Registry, path: ProxyPath, cls: Class[T]): ResolvingResult[T] = {
    val nsOption = DecodeUtils.getNamespaceByFqn(registry, path.ns)
    if (nsOption.isEmpty)
      return ResolvingResult.error(s"namespace not found ${path.ns}")
    val ns = nsOption.get
    path.element match {
      case e: TypeName =>
        val name: Option[DecodeName] = Some(e.typeName)
        val elements = (ns.units ++ ns.types ++ ns.components).filter(_.optionName.equals(name))
        if (elements.size == 1)
          ResolvingResult(Some(elements.head.asInstanceOf[T]))
        else
          ResolvingResult.error(s"must be exactly one element for $path, found: ${elements.size}")
      case e: ArrayTypePath =>
        val arrayTypeName: Option[DecodeName] = Some(e.mangledName)
        ns.types.filter(_.optionName.equals(arrayTypeName)) match {
          case s if s.size == 1 =>
            ResolvingResult(Some(s.head.asInstanceOf[T]))
          case s if s.isEmpty =>
            val arrayType = new ArrayTypeImpl(arrayTypeName, ns, None, MaybeProxy.proxy(e.baseTypePath), e.arraySize)
            ns.types = ns.types :+ arrayType
            val result = arrayType.baseType.resolve(registry, classOf[DecodeType])
            if (result.hasError)
              result.asInstanceOf[ResolvingResult[T]]
            else
              ResolvingResult(Some(arrayType.asInstanceOf[T]))
          case s =>
            ResolvingResult.error(s"must be exactly one element for $path, found: $s")
        }
      case _ =>
        ResolvingResult.empty
    }
  }
}