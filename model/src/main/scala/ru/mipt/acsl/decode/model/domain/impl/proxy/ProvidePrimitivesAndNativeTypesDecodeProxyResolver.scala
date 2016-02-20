package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.types.BerType
import ru.mipt.acsl.decode.model.domain.impl.types.GenericTypeSpecializedImpl
import ru.mipt.acsl.decode.model.domain.impl.types.OptionalType
import ru.mipt.acsl.decode.model.domain.impl.types.OrType
import ru.mipt.acsl.decode.model.domain.impl.{DecodeNameImpl, DecodeUtils}
import ru.mipt.acsl.decode.model.domain.impl.types.PrimitiveTypeImpl

import scala.collection.mutable

/**
  * Created by metadeus on 20.02.16.
  */
class ProvidePrimitivesAndNativeTypesDecodeProxyResolver extends DecodeProxyResolver {
  private val primitiveTypeByTypeKindBitSize: mutable.Map[DecodeName, PrimitiveType] = mutable.HashMap.empty
  private val nativeTypeByNameMap: mutable.Map[DecodeName, DecodeType] = mutable.HashMap.empty
  private val genericTypeSpecializedByTypeNameMap: mutable.Map[DecodeName, GenericTypeSpecialized] = mutable.HashMap.empty

  override def resolve[T <: Referenceable](registry: Registry, path: ProxyPath, cls: Class[T]): ResolvingResult[T] = {
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
              Some(DecodeNameImpl.newFromMangledName(s"${TypeKind.nameForTypeKind(e.typeKind)}:${e.bitSize}")),
              systemNamespace, None, e.typeKind, e.bitSize))
          val primitiveTypeName = primitiveType.optionName.get
          if (!systemNamespace.types.exists(_.optionName.exists(_.equals(primitiveTypeName))))
            systemNamespace.types = systemNamespace.types :+ primitiveType
          ResolvingResult(Some(primitiveType.asInstanceOf[T]))
        // Generic type
        case e: GenericTypeName =>
          val result = MaybeProxy.proxy(ProxyPath(nsFqn, e.typeName)).resolve(registry, classOf[GenericType])
          if (result.hasError)
            return result.asInstanceOf[ResolvingResult[T]]
          val genericType = result.resolvedObject.get
          val specializedType = genericTypeSpecializedByTypeNameMap.getOrElseUpdate(path.mangledName, {
            val specializedType = new GenericTypeSpecializedImpl(None, genericType.namespace, None,
              MaybeProxy.obj(genericType),
              e.genericArgumentPaths.map(_.map { arg => MaybeProxy.proxy[DecodeType](arg) }))
            systemNamespace.types = systemNamespace.types :+ specializedType
            specializedType
          })
          val argsResult = specializedType.genericTypeArguments.map(_.map(_.resolve(registry, classOf[DecodeType]))
            .getOrElse(ResolvingResult.empty[DecodeType]))
            .foldLeft(ResolvingResult.empty[DecodeType])(ResolvingResult.merge[DecodeType])
          if (argsResult.hasError)
            ResolvingResult[T](None, argsResult.messages)
          else
            ResolvingResult(Some(specializedType.asInstanceOf[T]))
        // Native type
        case e: TypeName if NativeType.MANGLED_TYPE_NAMES.contains(e.typeName.asMangledString) =>
          val name = DecodeNameImpl.newFromMangledName(e.typeName.asMangledString)
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
          ResolvingResult(Some(nativeType.asInstanceOf[T]))
        case _ =>
          ResolvingResult.empty
      }
    } else {
      ResolvingResult.empty
    }
  }
}
