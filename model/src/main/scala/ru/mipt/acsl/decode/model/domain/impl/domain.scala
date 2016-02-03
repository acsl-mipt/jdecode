package ru.mipt.acsl.decode.model.domain.impl

import java.net.URI

import ru.mipt.acsl.decode.model.domain.Aliases.DecodeMessageParameterToken
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.`type`.AbstractDecodeNameNamespaceOptionalInfoAware
import ru.mipt.acsl.decode.model.domain.impl.`type`.AbstractDecodeOptionalInfoAware
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeNamespaceImpl
import ru.mipt.acsl.decode.model.domain.impl.proxy.{ProvidePrimitivesAndNativeTypesDecodeProxyResolver, FindExistingDecodeProxyResolver}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
  * @author Artem Shein
  */
case class DecodeNameImpl(value: String) extends DecodeName {
  override def asMangledString: String = value
}

object DecodeNameImpl {
  def newFromSourceName(name: String) = DecodeNameImpl(DecodeName.mangleName(name))

  def newFromMangledName(name: String) = DecodeNameImpl(name)
}

case object TokenTypeWalker extends ((DecodeType, DecodeMessageParameterToken) => DecodeType) {
  private val optionWalker = TokenOptionTypeWalker
  override def apply(t: DecodeType, token: DecodeMessageParameterToken): DecodeType = optionWalker(t, token).get
}

case object TokenOptionTypeWalker extends ((DecodeType, DecodeMessageParameterToken) => Option[DecodeType]) {
  override def apply(t: DecodeType, token: DecodeMessageParameterToken): Option[DecodeType] = t match {
    case t: DecodeSubType => apply(t.baseType.obj, token)
    case t: DecodeArrayType =>
      if (!token.isRight)
        sys.error("invalid token")
      Some(t.baseType.obj)
    case t: DecodeStructType =>
      if (token.isRight)
        sys.error(s"invalid token ${token.right.get}")
      val name = token.left.get
      Some(t.fields.find(_.name.asMangledString == name)
        .getOrElse {
          sys.error(s"Field '$name' not found in struct '$t'")
        }.typeUnit.t.obj)
    case t: DecodeAliasType => apply(t.baseType.obj, token)
    case _ => None
  }
}

abstract class AbstractImmutableDecodeMessage(val component: DecodeComponent, val name: DecodeName, val id: Option[Int],
                                              info: Option[String], val parameters: Seq[DecodeMessageParameter]) extends AbstractDecodeMessage(info) {
  def optionName = Some(name)
}

case class DecodeComponentRefImpl(component: DecodeMaybeProxy[DecodeComponent], alias: Option[String] = None)
  extends DecodeComponentRef

class DecodeStatusMessageImpl(component: DecodeComponent, name: DecodeName, id: Option[Int], info: Option[String],
                              parameters: Seq[DecodeMessageParameter]) extends AbstractImmutableDecodeMessage(component, name, id, info, parameters) with DecodeStatusMessage

class DecodeEventMessageImpl(component: DecodeComponent, name: DecodeName, id: Option[Int], info: Option[String],
                             parameters: Seq[DecodeMessageParameter], val eventType: DecodeMaybeProxy[DecodeType])
  extends AbstractImmutableDecodeMessage(component, name, id, info, parameters) with DecodeEventMessage

class DecodeRegistryImpl(resolvers: DecodeProxyResolver*) extends DecodeRegistry {
  if (DecodeConstants.SYSTEM_NAMESPACE_FQN.size != 1)
    sys.error("not implemented")

  private val _rootNamespaces = new mutable.ArrayBuffer[DecodeNamespace]() +=
    new DecodeNamespaceImpl(DecodeConstants.SYSTEM_NAMESPACE_FQN.last, None)

  private val proxyResolvers = new mutable.ArrayBuffer[DecodeProxyResolver]() ++= resolvers

  def this() = this(new FindExistingDecodeProxyResolver(), new ProvidePrimitivesAndNativeTypesDecodeProxyResolver())

  def rootNamespaces: mutable.Buffer[DecodeNamespace] = _rootNamespaces

  def resolve[T <: DecodeReferenceable](uri: URI, cls: Class[T]): DecodeResolvingResult[T] = {
    for (resolver <- proxyResolvers) {
      val result = resolver.resolve(this, uri, cls)
      if (result.resolvedObject.isDefined)
        return result
    }
    SimpleDecodeResolvingResult.immutableEmpty()
  }
}

object DecodeRegistryImpl {
  def apply() = new DecodeRegistryImpl()
}

class DecodeMessageParameterRefWalker(var component: DecodeComponent, var structField: Option[DecodeStructField] = None,
                                      var subTokens: Seq[DecodeMessageParameterToken] = Seq.empty)
  extends DecodeMessageParameterRef {

  while (structField.isEmpty)
    walkOne()

  override def t: DecodeType = if (structField.isEmpty)
    component.baseType.get.obj
  else if (subTokens.isEmpty)
    structField.get.typeUnit.t.obj
  else
    subTokens.foldLeft(structField.get.typeUnit.t.obj)(TokenTypeWalker)

  private def walkOne(): Try[Unit] = {
    require(subTokens.nonEmpty)
    val token = subTokens.head
    val fail = () => { Failure(new IllegalStateException(s"can't walk $token for $this")) }
    subTokens = subTokens.tail
    if (structField.isDefined)
      return fail()
    if (token.isRight)
      return fail()
    val tokenString = token.left.get
    // try for sub components
    val subComponent = component.subComponents.find(tokenString == _.aliasOrMangledName)
    if (subComponent.isDefined) {
      component = subComponent.get.component.obj
      structField = None
      return Success()
    }
    if (component.baseType.isEmpty)
      return fail()
    val f = component.baseType.get.obj.fields.find(tokenString == _.name.asMangledString)
    if (f.isDefined) {
      structField = Some(f.get)
      return Success()
    }
    fail()
  }
}

class DecodeCommandImpl(val name: DecodeName, val id: Option[Int], info: Option[String],
                        val parameters: Seq[DecodeCommandParameter], val returnType: Option[DecodeMaybeProxy[DecodeType]])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeCommand {
  override def optionName: Option[DecodeName] = Some(name)
}

class DecodeLanguageImpl(name: DecodeName, namespace: DecodeNamespace, val isDefault: Boolean, info: Option[String])
  extends AbstractDecodeNameNamespaceOptionalInfoAware(name, namespace, info) with DecodeLanguage {
  override def accept[T](visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)
}

class DecodeMessageParameterImpl(val value: String, val info: Option[String] = None) extends DecodeMessageParameter