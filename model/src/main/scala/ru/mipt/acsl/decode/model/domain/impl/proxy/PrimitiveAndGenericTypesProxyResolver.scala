package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Fqn
import ru.mipt.acsl.decode.model.domain.impl.proxy.path.{GenericTypeName, ProxyPath}
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry
import ru.mipt.acsl.decode.model.domain.impl.types.{DecodeType, GenericType, GenericTypeSpecialized}
import ru.mipt.acsl.decode.model.domain.pure.Referenceable
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

import scala.collection.mutable

/**
  * Created by metadeus on 20.02.16.
  */
class PrimitiveAndGenericTypesProxyResolver extends DecodeProxyResolver {

  override def resolveElement(registry: Registry, path: ProxyPath): (Option[Referenceable], ResolvingResult) = {
    assert(Fqn.SystemNamespace.size == 1, "not implemented")
    val nsFqn = path.ns
    if (nsFqn.equals(Fqn.SystemNamespace)) {
      val systemNamespaceOptional = registry.findNamespace(Fqn.SystemNamespace)
      assert(systemNamespaceOptional.isDefined, "system namespace not found")
      val systemNamespace = systemNamespaceOptional.get
      path.element match {
        // Generic type
        case e: GenericTypeName =>
          val maybeProxy = MaybeProxy[GenericType](ProxyPath(nsFqn, e.typeName))
          val result = maybeProxy.resolve(registry)
          if (result.hasError)
            return (None, result)
          val genericType = maybeProxy.obj
          val name = path.element.mangledName
          val specializedType = systemNamespace.types.find(_.name.equals(name))
            .map(_.asInstanceOf[GenericTypeSpecialized]).getOrElse({
            val specializedType = GenericTypeSpecialized(name, genericType.namespace, LocalizedString.empty,
              MaybeProxy(genericType),
              e.genericArgumentPaths.map(_.map(arg => MaybeProxy[DecodeType](arg))))
            systemNamespace.types :+= specializedType
            specializedType
          })
          val argsResult = specializedType.genericTypeArgumentsProxy.map(_.map(_.resolve(registry))
            .getOrElse(Result.empty))
            .foldLeft(Result.empty)(_ ++ _)
          if (argsResult.hasError)
            (None, argsResult)
          else
            (Some(specializedType), Result.empty)
        case _ =>
          (None, Result.empty)
      }
    } else {
      (None, Result.empty)
    }
  }
}
