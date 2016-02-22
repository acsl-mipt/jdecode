package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.types.BerType
import ru.mipt.acsl.decode.model.domain.impl.types.GenericTypeSpecializedImpl
import ru.mipt.acsl.decode.model.domain.impl.types.OptionalType
import ru.mipt.acsl.decode.model.domain.impl.types.OrType
import ru.mipt.acsl.decode.model.domain.impl.{ElementName, DecodeUtils}
import ru.mipt.acsl.decode.model.domain.impl.types.PrimitiveTypeImpl
import ru.mipt.acsl.decode.model.domain.proxy._
import ru.mipt.acsl.decode.model.domain.proxy.aliases._

import scala.collection.mutable

/**
  * Created by metadeus on 20.02.16.
  */
class PrimitiveAndNativeTypesProxyResolver extends DecodeProxyResolver {
  private val primitiveTypeByTypeKindBitSize: mutable.Map[ElementName, PrimitiveType] = mutable.HashMap.empty
  private val nativeTypeByNameMap: mutable.Map[ElementName, DecodeType] = mutable.HashMap.empty
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
              Some(ElementName.newFromMangledName(s"${TypeKind.nameForTypeKind(e.typeKind)}:${e.bitSize}")),
              systemNamespace, None, e.typeKind, e.bitSize))
          val primitiveTypeName = primitiveType.optionName.get
          if (!systemNamespace.types.exists(_.optionName.exists(_.equals(primitiveTypeName))))
            systemNamespace.types = systemNamespace.types :+ primitiveType
          (Some(primitiveType), ResolvingResult.empty)
        // Generic type
        case e: GenericTypeName =>
          val maybeProxy = MaybeProxy.proxy[GenericType](ProxyPath(nsFqn, e.typeName))
          val result = maybeProxy.resolve(registry)
          if (result.hasError)
            return (None, result)
          val genericType = maybeProxy.obj
          val specializedType = genericTypeSpecializedByTypeNameMap.getOrElseUpdate(path.mangledName, {
            val specializedType = new GenericTypeSpecializedImpl(None, genericType.namespace, None,
              MaybeProxy.obj(genericType),
              e.genericArgumentPaths.map(_.map { arg => MaybeProxy.proxy[DecodeType](arg) }))
            systemNamespace.types = systemNamespace.types :+ specializedType
            specializedType
          })
          val argsResult = specializedType.genericTypeArguments.map(_.map(_.resolve(registry))
            .getOrElse(ResolvingResult.empty))
            .foldLeft(ResolvingResult.empty)(_ ++ _)
          if (argsResult.hasError)
            (None, argsResult)
          else
            (Some(specializedType), ResolvingResult.empty)
        // Native type
        case e: TypeName if NativeType.MANGLED_TYPE_NAMES.contains(e.typeName.asMangledString) =>
          val name = ElementName.newFromMangledName(e.typeName.asMangledString)
          val nativeType = nativeTypeByNameMap.getOrElseUpdate(path.mangledName, {
            name match {
              case BerType.MANGLED_NAME =>
                BerType(systemNamespace, None)
              case OptionalType.MANGLED_NAME =>
                new OptionalType(Some(name), systemNamespace, None)
              case OrType.MANGLED_NAME =>
                new OrType(Some(name), systemNamespace, None)
              case _ =>
                sys.error("not implemented")
            }
          })
          if (!systemNamespace.types.exists(_.optionName.equals(nativeType.optionName)))
            systemNamespace.types = systemNamespace.types :+ nativeType
          (Some(nativeType), ResolvingResult.empty)
        case _ =>
          (None, ResolvingResult.empty)
      }
    } else {
      (None, ResolvingResult.empty)
    }
  }
}
