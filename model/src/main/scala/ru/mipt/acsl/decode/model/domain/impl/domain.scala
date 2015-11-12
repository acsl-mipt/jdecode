package ru.mipt.acsl.decode.model.domain.impl

import java.net.URI

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeNamespaceImpl
import ru.mipt.acsl.decode.model.domain.impl.proxy.{ProvidePrimitivesAndNativeTypesDecodeProxyResolver, FindExistingDecodeProxyResolver}

import scala.collection.mutable

/**
  * @author Artem Shein
  */
class TokenWalker(val token: Either[String, Int]) extends DecodeTypeVisitor[Option[DecodeType]] {
  override def visit(primitiveType: DecodePrimitiveType) = None

  override def visit(nativeType: DecodeNativeType) = None

  override def visit(subType: DecodeSubType) = subType.baseType.obj.accept(this)

  override def visit(enumType: DecodeEnumType) = None

  override def visit(arrayType: DecodeArrayType) = {
    if (!token.isRight)
      sys.error("invalid token")
    Some(arrayType.baseType.obj)
  }

  override def visit(structType: DecodeStructType) = {
    if (token.isLeft)
      sys.error("invalid token")
    val name = token.left
    Some(structType.fields.find(_.name.asString == name)
      .getOrElse({
      throw new AssertionError(
        String.format("Field '%s' not found in struct '%s'", name, structType))
    }).fieldType.obj)
  }

  override def visit(typeAlias: DecodeAliasType) =  typeAlias.baseType.obj.accept(this)

  override def visit(genericType: DecodeGenericType) = None

  override def visit(genericTypeSpecialized: DecodeGenericTypeSpecialized) = None
}

abstract class AbstractImmutableDecodeMessage(val component: DecodeComponent, val name: DecodeName, val id: Option[Int],
  info: Option[String], val parameters: Seq[DecodeMessageParameter]) extends AbstractDecodeMessage(info) {
  def optionalName = Some(name)
}

class DecodeComponentRefImpl(val component: DecodeMaybeProxy[DecodeComponent], val alias: Option[String] = None)
  extends DecodeComponentRef

class DecodeStatusMessageImpl(component: DecodeComponent, name: DecodeName, id: Option[Int], info: Option[String],
  parameters: Seq[DecodeMessageParameter]) extends AbstractImmutableDecodeMessage(component, name, id, info, parameters) with DecodeStatusMessage

class DecodeEventMessageImpl(component: DecodeComponent, name: DecodeName, id: Option[Int], info: Option[String],
                                  parameters: Seq[DecodeMessageParameter], val eventType: DecodeMaybeProxy[DecodeType])
  extends AbstractImmutableDecodeMessage(component, name, id, info, parameters) with DecodeEventMessage

class DecodeRegistryImpl extends DecodeRegistry {
  private val _rootNamespaces = new mutable.ArrayBuffer[DecodeNamespace]()
  private val _proxyResolvers = new mutable.ArrayBuffer[DecodeProxyResolver]()
  def this(resolvers: DecodeProxyResolver*) = {
    this()
    _proxyResolvers ++= resolvers
    if (DecodeConstants.SYSTEM_NAMESPACE_FQN.size != 1)
      sys.error("not implemented")
    _rootNamespaces += DecodeNamespaceImpl.apply(DecodeConstants.SYSTEM_NAMESPACE_FQN.last,
      Option.empty)
  }
  def rootNamespaces: mutable.Buffer[DecodeNamespace] = _rootNamespaces
  def resolve[T <: DecodeReferenceable](uri: URI, cls: Class[T]): DecodeResolvingResult[T] = {
    for (resolver <- _proxyResolvers)
    {
      val result = resolver.resolve(this, uri, cls)
      if (result.resolvedObject.isDefined)
      {
        return result
      }
    }
    SimpleDecodeResolvingResult.immutableEmpty()
  }
}

object DecodeRegistryImpl {
  def apply() = new DecodeRegistryImpl(new FindExistingDecodeProxyResolver(), new ProvidePrimitivesAndNativeTypesDecodeProxyResolver())
}

class DecodeComponentWalker(var component: DecodeComponent) {
  private var t: Option[DecodeType] = Option.empty[DecodeType]

  def `type`: Option[DecodeType] = t

  def walk(token: Either[String, Int]) {
    if (t.isDefined)
    {
      // must not return null
      t = t.get.accept(new TokenWalker(token))
      if (t.isEmpty)
        sys.error("fail")
    }
    else
    {
      if (!token.isLeft)
        sys.error("wtf")
      val stringToken = token.left
      val subComponent = component.subComponents.find(cr => {
        val alias = cr.alias
        (alias.isDefined && alias.get == stringToken.get) | cr.component.obj.name.asString() == stringToken.get
      })
      if (subComponent.isDefined)
      {
        component = subComponent.get.component.obj
      }
      else
      {
        if (component.baseType.isDefined)
          sys.error("wtf")
        t = Some(component.baseType.get.obj)
        walk(token)
      }
    }
  }
}