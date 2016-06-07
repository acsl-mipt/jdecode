package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.decode.model
import ru.mipt.acsl.decode.model.expr.ConstExpr
import ru.mipt.acsl.decode.model.registry.Registry
import ru.mipt.acsl.decode.model.types.{Const, DecodeType, EnumType, StructType}
import ru.mipt.acsl.decode.model.{Referenceable, component, registry}
import ru.mipt.acsl.modeling.{ErrorLevel, Message}

/**
  * @author Artem Shein
  */
sealed trait MaybeProxy extends Referenceable {

  def resolve(registry: Registry): ResolvingMessages = isResolved match {
    case true => Result.empty
    case _ =>
      val resolvingResult = registry.resolveElement(proxy.path)
      resolvingResult.result match {
        case Some(o) => resolvingResult.messages ++= resolveTo(o)
        case _ => sys.error(s"can't resolve proxy $proxy")
      }
      resolvingResult.messages
  }

  def resolveTo(obj: Referenceable): ResolvingMessages

  def proxy: Proxy

  def isResolved: Boolean
/*
  def isProxy: Boolean = v.isLeft

  def isResolved: Boolean = v.isRight

  def obj: T = v.right.getOrElse(sys.error(s"assertion error for $proxy"))

  def proxy: Proxy = v.left.getOrElse(sys.error("assertion error"))

  override def toString: String = s"MaybeProxy{${if (isProxy) proxy else obj}}"*/
}

object MaybeProxy {

  sealed trait TypeProxy extends MaybeProxy {

    def obj: DecodeType

  }


  final case class Type(var v: Either[Proxy, DecodeType]) extends TypeProxy {

    override def isResolved: Boolean = v.isRight

    override def obj: DecodeType = v.right.getOrElse(sys.error("proxy is not resolved"))

    override def proxy: Proxy = v.left.get

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = obj match {
      case obj: DecodeType => v = Right(obj); ResolvingMessages.empty
      case _ => ResolvingMessages(Message(ErrorLevel, s"$obj is not a type"))
    }

  }

  final case class Struct(var v: Either[Proxy, StructType]) extends TypeProxy {

    override def isResolved: Boolean = v.isRight

    override def obj: StructType = v.right.get

    override def proxy: Proxy = v.left.get

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = obj match {
      case obj: StructType => v = Right(obj); ResolvingMessages.empty
      case _ => ResolvingMessages(Message(ErrorLevel, s"$obj is not a StructType"))
    }

  }

  final case class Enum(var v: Either[Proxy, EnumType]) extends TypeProxy {

    override def isResolved: Boolean = v.isRight

    override def obj: EnumType = v.right.get

    override def proxy: Proxy = v.left.get

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = obj match {
      case obj: EnumType => v = Right(obj); ResolvingMessages.empty
      case _ => ResolvingMessages(Message(ErrorLevel, s"$obj is not an EnumType"))
    }

  }

  final case class Measure(var v: Either[Proxy, registry.Measure]) extends MaybeProxy {

    def obj: registry.Measure = v.right.getOrElse(sys.error("proxy is not resolved"))

    override def isResolved: Boolean = v.isRight

    override def proxy: Proxy = v.left.get

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = obj match {
      case obj: registry.Measure => v = Right(obj); ResolvingMessages.empty
      case _ => ResolvingMessages(Message(ErrorLevel, s"$obj is not a Measure"))
    }

  }

  final case class Component(var v: Either[Proxy, component.Component]) extends MaybeProxy {

    def obj: component.Component = v.right.getOrElse(sys.error("proxy is not resolved"))

    override def isResolved: Boolean = v.isRight

    override def proxy: Proxy = v.left.get

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = obj match {
      case obj: component.Component => v = Right(obj); ResolvingMessages.empty
      case _ => ResolvingMessages(Message(ErrorLevel, s"$obj is not a Component"))
    }

  }

  final case class Referenceable(var v: Either[Proxy, model.Referenceable]) extends MaybeProxy {

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = { v = Right(obj); ResolvingMessages.empty }

    override def proxy: Proxy = v.left.get

    override def isResolved: Boolean = v.isRight
  }

  /*def apply[T <: Referenceable : ClassTag](proxy: Proxy): MaybeProxy[T] = new MaybeProxy[T](Left(proxy))

  def apply[T <: Referenceable : ClassTag](path: ProxyPath): MaybeProxy[T] = apply(Proxy(path))

  def apply[T <: Referenceable : ClassTag](namespaceFqn: Fqn, name: ProxyElementName): MaybeProxy[T] =
    apply(ProxyPath(namespaceFqn, name))

  def apply[T <: Referenceable : ClassTag](obj: T) = new MaybeProxy[T](Right(obj))

  def proxyForLiteral[T <: Referenceable : ClassTag](value: String): MaybeProxy[T] =
    apply(Proxy(ProxyPath.fromLiteral(value)))

  */
}
