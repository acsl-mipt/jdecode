package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.aliases.{ValidatingResult, MessageParameterToken}
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractNameNamespaceOptionalInfoAware
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractDecodeOptionalInfoAware
import ru.mipt.acsl.decode.model.domain.impl.types.NamespaceImpl
import ru.mipt.acsl.decode.model.domain.impl.proxy.{PrimitiveAndNativeTypesProxyResolver, ExistingElementsProxyResolver}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.proxy.{DecodeProxyResolver, MaybeProxy, ProxyPath}
import ru.mipt.acsl.decode.modeling.ErrorLevel
import ru.mipt.acsl.decode.modeling.impl.Message

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}
import scala.collection.immutable
import scala.reflect._

/**
  * @author Artem Shein
  */
private case class ElementNameImpl(value: String) extends ElementName {
  override def asMangledString: String = value
}

object ElementName {
  def mangleName(name: String): String = {
    var result = name
    if (result.startsWith("^")) {
      result = result.substring(1)
    }
    result = "[ \\\\^]".r.replaceAllIn(result, "")
    if (result.isEmpty)
      sys.error("invalid name")
    result
  }

  def newFromSourceName(name: String): ElementName = ElementNameImpl(ElementName.mangleName(name))

  def newFromMangledName(name: String): ElementName = ElementNameImpl(name)
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

abstract class AbstractImmutableMessage(val component: Component, val name: ElementName, val id: Option[Int],
                                        info: Option[String]) extends AbstractMessage(info) {
  def optionName = Some(name)
}

case class ComponentRefImpl(component: MaybeProxy[Component], alias: Option[String] = None)
  extends ComponentRef

class StatusMessageImpl(component: Component, name: ElementName, id: Option[Int], info: Option[String],
                        val parameters: Seq[MessageParameter], val priority: Option[Int] = None)
  extends AbstractImmutableMessage(component, name, id, info) with StatusMessage

class EventMessageImpl(component: Component, name: ElementName, id: Option[Int], info: Option[String],
                       val fields: Seq[Either[MessageParameter, Parameter]], val baseType: MaybeProxy[DecodeType])
  extends AbstractImmutableMessage(component, name, id, info) with EventMessage

private class RegistryImpl(val name: ElementName, resolvers: DecodeProxyResolver*) extends Registry {
  if (DecodeConstants.SYSTEM_NAMESPACE_FQN.size != 1)
    sys.error("not implemented")

  var rootNamespaces: immutable.Seq[Namespace] = immutable.Seq(
    new NamespaceImpl(DecodeConstants.SYSTEM_NAMESPACE_FQN.last, None))

  private val proxyResolvers = resolvers.to[immutable.Seq]

  def this() = this(ElementName.newFromMangledName("GlobalRegistry"),
    new ExistingElementsProxyResolver(),
    new PrimitiveAndNativeTypesProxyResolver())

  override def resolveElement[T <: Referenceable](path: ProxyPath)(implicit ct: ClassTag[T]): (Option[T], ResolvingResult) = {
    for (resolver <- proxyResolvers) {
      val result = resolver.resolveElement(this, path)
      for (obj <- result._1)
        return obj match {
          case ct(o) =>
            (Some(o), result._2)
          case o =>
            (None, result._2 :+ Message(ErrorLevel, s"invalid type ${o.getClass}, expected ${ct.runtimeClass}"))
        }
    }
    (None, Seq(Message(ErrorLevel, s"path $path can not be resolved")))
  }

  def resolve(): ResolvingResult = resolve(this)

  override def resolve(registry: Registry): ResolvingResult =
    registry.rootNamespaces.flatMap(_.resolve(registry))

  override def optionName: Option[ElementName] = Some(name)

  override def validate(registry: Registry): ValidatingResult =
    registry.rootNamespaces.flatMap(_.validate(registry))
}

object Registry {
  def apply(): Registry = new RegistryImpl()
}

class MessageParameterRefWalker(var component: Component, var structField: Option[StructField] = None,
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

class CommandImpl(val name: ElementName, val id: Option[Int], info: Option[String],
                  val parameters: immutable.Seq[Parameter],
                  val returnType: Option[MaybeProxy[DecodeType]])
  extends AbstractDecodeOptionalInfoAware(info) with Command {
  override def optionName: Option[ElementName] = Some(name)
}

class LanguageImpl(name: ElementName, namespace: Namespace, val isDefault: Boolean, info: Option[String])
  extends AbstractNameNamespaceOptionalInfoAware(name, namespace, info) with Language {
}

class MessageParameterImpl(val value: String, val info: Option[String] = None) extends MessageParameter