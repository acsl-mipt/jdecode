package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.aliases.{MessageParameterToken, ValidatingResult}
import ru.mipt.acsl.decode.model.domain.component.messages._
import ru.mipt.acsl.decode.model.domain.component.{Command, Component, ComponentRef, Parameter}
import ru.mipt.acsl.decode.model.domain.impl.proxy.{ExistingElementsProxyResolver, PrimitiveAndGenericTypesProxyResolver}
import ru.mipt.acsl.decode.model.domain.impl.types.{Fqn, ArrayType => _, StructType => _, SubType => _, _}
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.proxy.{DecodeProxyResolver, MaybeProxy, ProxyPath}
import ru.mipt.acsl.decode.model.domain.registry.{Language, Registry}
import ru.mipt.acsl.decode.model.domain.types.StructField
import ru.mipt.acsl.decode.model.domain.types._
import ru.mipt.acsl.decode.modeling.ErrorLevel
import ru.mipt.acsl.decode.modeling.impl.Message

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * @author Artem Shein
  */
object DecodeConstants {
  val SYSTEM_NAMESPACE_FQN: Fqn = Fqn.newFromSource("decode")
}

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

private abstract class AbstractMessage(info: Option[String]) extends AbstractOptionalInfoAware(info) with TmMessage

private abstract class AbstractImmutableMessage(val component: Component, val name: ElementName, val id: Option[Int],
                                        info: Option[String]) extends AbstractMessage(info) {
  def optionName = Some(name)
}

private class ComponentRefImpl(val component: MaybeProxy[Component], val alias: Option[String] = None)
  extends ComponentRef

object ComponentRef {
  def apply(component: MaybeProxy[Component], alias: Option[String] = None): ComponentRef =
    new ComponentRefImpl(component, alias)
}

private class StatusMessageImpl(component: Component, name: ElementName, id: Option[Int], info: Option[String],
                                val parameters: Seq[MessageParameter], val priority: Option[Int] = None)
  extends AbstractImmutableMessage(component, name, id, info) with StatusMessage

object StatusMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: Option[String],
            parameters: Seq[MessageParameter], priority: Option[Int] = None): StatusMessage =
    new StatusMessageImpl(component, name, id, info, parameters, priority)
}

private class EventMessageImpl(component: Component, name: ElementName, id: Option[Int], info: Option[String],
                               val fields: Seq[Either[MessageParameter, Parameter]], val baseType: MaybeProxy[DecodeType])
  extends AbstractImmutableMessage(component, name, id, info) with EventMessage

object EventMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: Option[String],
            fields: Seq[Either[MessageParameter, Parameter]], baseType: MaybeProxy[DecodeType]): EventMessage =
    new EventMessageImpl(component, name, id, info, fields, baseType)
}

private class RegistryImpl(val name: ElementName, resolvers: DecodeProxyResolver*) extends Registry {
  if (DecodeConstants.SYSTEM_NAMESPACE_FQN.size != 1)
    sys.error("not implemented")

  var rootNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty

  private val proxyResolvers = resolvers.to[immutable.Seq]

  def this() = this(ElementName.newFromMangledName("GlobalRegistry"),
    new ExistingElementsProxyResolver(),
    new PrimitiveAndGenericTypesProxyResolver())

  override def component(fqn: String): Option[Component] = {
    val dotPos = fqn.lastIndexOf('.')
    val namespaceOptional = namespace(fqn.substring(0, dotPos))
    if (namespaceOptional.isEmpty)
    {
      return None
    }
    val componentName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    namespaceOptional.get.components.find(_.name == componentName)
  }

  override def namespace(fqn: String): Option[Namespace] = {
    var currentNamespaces: Option[Seq[Namespace]] = Some(rootNamespaces)
    var currentNamespace: Option[Namespace] = None
    "\\.".r.split(fqn).foreach(nsName => {
      if (currentNamespaces.isEmpty)
      {
        return None
      }
      val decodeName = ElementName.newFromMangledName(nsName)
      currentNamespace = currentNamespaces.get.find(_.name == decodeName)
      if (currentNamespace.isDefined)
      {
        currentNamespaces = Some(currentNamespace.get.subNamespaces)
      }
      else
      {
        currentNamespaces = None
      }
    })
    currentNamespace
  }

  override def eventMessage(fqn: String): Option[EventMessage] = {
    val dotPos = fqn.lastIndexOf('.')
    val decodeName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    component(fqn.substring(0, dotPos)).map(_.eventMessages.find(_.name == decodeName).orNull)
  }

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

  override def statusMessage(fqn: String): Option[StatusMessage] = {
    val dotPos = fqn.lastIndexOf('.')
    val decodeName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    component(fqn.substring(0, dotPos)).map(_.statusMessages.find(_.name == decodeName).orNull)
  }

  def resolve(): ResolvingResult = resolve(this)

  override def resolve(registry: Registry): ResolvingResult =
    registry.rootNamespaces.flatMap(_.resolve(registry))

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

private class CommandImpl(val name: ElementName, val id: Option[Int], info: Option[String],
                          val parameters: immutable.Seq[Parameter], val returnType: Option[MaybeProxy[DecodeType]])
  extends AbstractOptionalInfoAware(info) with Command

object Command {
  def apply(name: ElementName, id: Option[Int], info: Option[String],
            parameters: immutable.Seq[Parameter], returnType: Option[MaybeProxy[DecodeType]]): Command =
    new CommandImpl(name, id, info, parameters, returnType)
}

private class LanguageImpl(name: ElementName, namespace: Namespace, val isDefault: Boolean, info: Option[String])
  extends AbstractNameNamespaceOptionalInfoAware(name, namespace, info) with Language

object Language {
  def apply(name: ElementName, namespace: Namespace, isDefault: Boolean, info: Option[String]): Language =
    new LanguageImpl(name, namespace, isDefault, info)
}

private class MessageParameterImpl(val value: String, val info: Option[String] = None) extends MessageParameter {
  def ref(component: Component): MessageParameterRef =
    new MessageParameterRefWalker(component, None, tokens)

  private def tokens: Seq[MessageParameterToken] = ParameterWalker(this).tokens
}

object MessageParameter {
  def apply(value: String, info: Option[String] = None): MessageParameter =
    new MessageParameterImpl(value, info)
}