package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.types.GenericTypeSpecializedImpl
import ru.mipt.acsl.decode.model.domain.impl.{DecodeUtils, ElementName}
import ru.mipt.acsl.decode.model.domain.impl.types.{GenericTypeSpecialized, PrimitiveTypeImpl}
import ru.mipt.acsl.decode.model.domain.proxy._
import ru.mipt.acsl.decode.model.domain.proxy.aliases._

import scala.collection.mutable

/**
  * Created by metadeus on 20.02.16.
  */
class PrimitiveAndGenericTypesProxyResolver extends DecodeProxyResolver {
  private val primitiveTypeByTypeKindBitSize: mutable.Map[ElementName, PrimitiveType] = mutable.HashMap.empty
  private val genericTypeSpecializedByTypeNameMap: mutable.Map[ElementName, GenericTypeSpecialized] = mutable.HashMap.empty

  override def resolveElement(registry: Registry, path: ProxyPath): (Option[Referenceable], ResolvingResult) = {
    assert(DecodeConstants.SYSTEM_NAMESPACE_FQN.size == 1, "not implemented")
    val nsFqn = path.ns
    if (nsFqn.equals(DecodeConstants.SYSTEM_NAMESPACE_FQN)) {
      val systemNamespaceOptional = DecodeUtils.getNamespaceByFqn(registry, DecodeConstants.SYSTEM_NAMESPACE_FQN)
      assert(systemNamespaceOptional.isDefined, "system namespace not found")
      val systemNamespace = systemNamespaceOptional.get
      path.element match {
        // Primitive type
        case e: PrimitiveTypeName =>
          val primitiveType = primitiveTypeByTypeKindBitSize.getOrElseUpdate(path.mangledName,
            new PrimitiveTypeImpl(
              ElementName.newFromMangledName(s"${TypeKind.nameForTypeKind(e.typeKind)}:${e.bitSize}"),
              systemNamespace, None, e.typeKind, e.bitSize))
          val primitiveTypeName = primitiveType.name
          if (!systemNamespace.types.exists(_.name.equals(primitiveTypeName)))
            systemNamespace.types = systemNamespace.types :+ primitiveType
          (Some(primitiveType), Result.empty)
        // Generic type
        case e: GenericTypeName =>
          val maybeProxy = MaybeProxy.proxy[GenericType](ProxyPath(nsFqn, e.typeName))
          val result = maybeProxy.resolve(registry)
          if (result.hasError)
            return (None, result)
          val genericType = maybeProxy.obj
          val name = path.element.mangledName
          val specializedType = systemNamespace.types.find(_.name.equals(name))
            .map(_.asInstanceOf[GenericTypeSpecialized]).getOrElse({
            val specializedType = GenericTypeSpecialized(name, genericType.namespace, None,
              MaybeProxy.obj(genericType),
              e.genericArgumentPaths.map(_.map(arg => MaybeProxy.proxy[DecodeType](arg))))
            systemNamespace.types = systemNamespace.types :+ specializedType
            specializedType
          })
          val argsResult = specializedType.genericTypeArguments.map(_.map(_.resolve(registry))
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
