package ru.mipt.acsl.decode.model.domain.impl

import java.net.URI

import ru.mipt.acsl.decode.model.domain.aliases.MessageParameterToken
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.`type`.AbstractNameNamespaceOptionalInfoAware
import ru.mipt.acsl.decode.model.domain.impl.`type`.AbstractDecodeOptionalInfoAware
import ru.mipt.acsl.decode.model.domain.impl.`type`.NamespaceImpl
import ru.mipt.acsl.decode.model.domain.impl.proxy.{ProvidePrimitivesAndNativeTypesDecodeProxyResolver, FindExistingDecodeProxyResolver}

import scala.util.{Failure, Success, Try}
import scala.collection.immutable

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

case object TokenTypeWalker extends ((DecodeType, MessageParameterToken) => DecodeType) {
  private val optionWalker = TokenOptionTypeWalker
  override def apply(t: DecodeType, token: MessageParameterToken): DecodeType = optionWalker(t, token).get
}

case object TokenOptionTypeWalker extends ((DecodeType, MessageParameterToken) => Option[DecodeType]) {
  override def apply(t: DecodeType, token: MessageParameterToken): Option[DecodeType] = t match {
    case t: SubType => apply(t.baseType.obj, token)
    case t: ArrayType =>
      if (!token.isRight)
        sys.error("invalid token")
      Some(t.baseType.obj)
    case t: StructType =>
      if (token.isRight)
        sys.error(s"invalid token ${token.right.get}")
      val name = token.left.get
      Some(t.fields.find(_.name.asMangledString == name)
        .getOrElse {
          sys.error(s"Field '$name' not found in struct '$t'")
        }.typeUnit.t.obj)
    case t: AliasType => apply(t.baseType.obj, token)
    case _ => None
  }
}

abstract class AbstractImmutableMessage(val component: Component, val name: DecodeName, val id: Option[Int],
                                        info: Option[String]) extends AbstractMessage(info) {
  def optionName = Some(name)
}

case class DecodeComponentRefImpl(component: MaybeProxy[Component], alias: Option[String] = None)
  extends DecodeComponentRef

class StatusMessageImpl(component: Component, name: DecodeName, id: Option[Int], info: Option[String],
                        val parameters: Seq[MessageParameter], val priority: Option[Int] = None)
  extends AbstractImmutableMessage(component, name, id, info) with StatusMessage

class EventMessageImpl(component: Component, name: DecodeName, id: Option[Int], info: Option[String],
                       val fields: Seq[Either[MessageParameter, Parameter]], val baseType: MaybeProxy[DecodeType])
  extends AbstractImmutableMessage(component, name, id, info) with EventMessage

class RegistryImpl(resolvers: DecodeProxyResolver*) extends Registry {
  if (DecodeConstants.SYSTEM_NAMESPACE_FQN.size != 1)
    sys.error("not implemented")

  var rootNamespaces: immutable.Seq[Namespace] = immutable.Seq(
    new NamespaceImpl(DecodeConstants.SYSTEM_NAMESPACE_FQN.last, None))

  private val proxyResolvers = resolvers.to[immutable.Seq]

  def this() = this(new FindExistingDecodeProxyResolver(), new ProvidePrimitivesAndNativeTypesDecodeProxyResolver())

  def resolve[T <: Referenceable](uri: URI, cls: Class[T]): ResolvingResult[T] = {
    for (resolver <- proxyResolvers) {
      val result = resolver.resolve(this, uri, cls)
      if (result.resolvedObject.isDefined)
        return result
    }
    ResolvingResult.empty
  }
}

object RegistryImpl {
  def apply() = new RegistryImpl()
}

class MessageParameterRefWalker(var component: Component, var structField: Option[DecodeStructField] = None,
                                var subTokens: Seq[MessageParameterToken] = Seq.empty)
  extends MessageParameterRef {

  while (structField.isEmpty)
    walkOne()

  override def t: DecodeType = if (structField.isEmpty)
    component.baseType.get.obj
  else if (subTokens.isEmpty)
    structField.get.typeUnit.t.obj
  else
    subTokens.foldLeft(structField.get.typeUnit.t.obj)(TokenTypeWalker)

  private def findSubComponent(tokenString: String): Option[Try[Unit]] =
    component.subComponents.find(tokenString == _.aliasOrMangledName).map { subComponent =>
      component = subComponent.component.obj
      structField = None
      Success()
    }

  private def findBaseTypeField(tokenString: String): Option[Try[Unit]] =
    component.baseType.flatMap(_.obj.fields.find(tokenString == _.name.asMangledString).map { f =>
      structField = Some(f)
      Success()
    })

  private def findTokenString(token: MessageParameterToken): Option[Try[Unit]] =
    token.left.toOption.flatMap { tokenString =>
      findSubComponent(tokenString).orElse(findBaseTypeField(tokenString))
    }

  private def walkOne(): Try[Unit] = {
    require(subTokens.nonEmpty)
    val token = subTokens.head
    val fail = () => Try[Unit] { Failure(new IllegalStateException(s"can't walk $token for $this")) }
    subTokens = subTokens.tail
    structField match {
      case Some(_) => fail()
      case _ => findTokenString(token).getOrElse { fail() }
    }
  }
}

class DecodeCommandImpl(val name: DecodeName, val id: Option[Int], info: Option[String],
                        val parameters: immutable.Seq[Parameter],
                        val returnType: Option[MaybeProxy[DecodeType]])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeCommand {
  override def optionName: Option[DecodeName] = Some(name)
}

class LanguageImpl(name: DecodeName, namespace: Namespace, val isDefault: Boolean, info: Option[String])
  extends AbstractNameNamespaceOptionalInfoAware(name, namespace, info) with Language {
  override def accept[T](visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)
}

class MessageParameterImpl(val value: String, val info: Option[String] = None) extends MessageParameter