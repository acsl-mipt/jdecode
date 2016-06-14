package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.decode.model
import ru.mipt.acsl.decode.model.registry.Registry
import ru.mipt.acsl.decode.model.types.{DecodeType, EnumType, StructType}
import ru.mipt.acsl.decode.model.{Referenceable, _}

/**
  * @author Artem Shein
  */

object MaybeProxyCompanion {

  final case class Struct(var v: Either[Proxy, StructType]) extends MaybeTypeProxy {

    override def isResolved: Boolean = v.isRight

    override def obj: StructType = v.right.get

    override def proxy: Proxy = v.left.get

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = obj match {
      case obj: StructType => v = Right(obj); ResolvingMessages.newInstance()
      case _ => ResolvingMessages.newInstance(Message.newInstance(Level.ERROR, s"$obj is not a StructType"))
    }

  }

  final case class Enum(var v: Either[Proxy, EnumType]) extends MaybeTypeProxy {

    override def isResolved: Boolean = v.isRight

    override def obj: EnumType = v.right.get

    override def proxy: Proxy = v.left.get

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = obj match {
      case obj: EnumType => v = Right(obj); ResolvingMessages.newInstance()
      case _ => ResolvingMessages.newInstance(Message.newInstance(Level.ERROR, s"$obj is not an EnumType"))
    }

  }

  final case class Measure(var v: Either[Proxy, registry.Measure]) extends MaybeProxy {

    def obj: registry.Measure = v.right.getOrElse(sys.error("proxy is not resolved"))

    override def isResolved: Boolean = v.isRight

    override def proxy: Proxy = v.left.get

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = obj match {
      case obj: registry.Measure => v = Right(obj); ResolvingMessages.newInstance()
      case _ => ResolvingMessages.newInstance(Message.newInstance(Level.ERROR, s"$obj is not a Measure"))
    }

  }

  final case class Component(var v: Either[Proxy, component.Component]) extends MaybeProxy {

    def obj: component.Component = v.right.getOrElse(sys.error("proxy is not resolved"))

    override def isResolved: Boolean = v.isRight

    override def proxy: Proxy = v.left.get

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = obj match {
      case obj: component.Component => v = Right(obj); ResolvingMessages.newInstance()
      case _ => ResolvingMessages.newInstance(Message.newInstance(Level.ERROR, s"$obj is not a Component"))
    }

  }

  final case class Referenceable(var v: Either[Proxy, model.Referenceable]) extends MaybeProxy {

    override def resolveTo(obj: model.Referenceable): ResolvingMessages = { v = Right(obj); ResolvingMessages.newInstance() }

    override def proxy: Proxy = v.left.get

    override def isResolved: Boolean = v.isRight

    override def obj(): model.Referenceable = v.right.get
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
