package ru.mipt.acsl.decode.model.types

import java.util
import java.util.Optional
import scala.collection.JavaConversions._

import scala.collection.JavaConversions._
import org.jetbrains.annotations.Nullable
import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.proxy.{MaybeProxyCompanion, MaybeTypeProxy}

/**
  * @author Artem Shein
  */
trait GenericTypeSpecialized extends DecodeType {

  def genericTypeProxy: MaybeTypeProxy

  def genericType: DecodeType = genericTypeProxy.obj

  def genericTypeArgumentsProxy: util.List[MaybeTypeProxy]

  def genericTypeArguments: util.List[DecodeType] = genericTypeArgumentsProxy.map(_.obj)

  def systemName: String = "@" + hashCode()

  override def toString: String =
    s"${this.getClass}{alias = $alias, namespace = $namespace, genericTypeProxy = $genericTypeProxy," +
      s" genericTypeArgumentsProxy = $genericTypeArgumentsProxy, typeParameters = $typeParameters}"

}

object GenericTypeSpecialized {

  private class GenericTypeSpecializedImpl(@Nullable val _alias: Alias.NsType, var namespace: Namespace,
                                           val genericTypeProxy: MaybeTypeProxy,
                                           val genericTypeArgumentsProxy: util.List[MaybeTypeProxy],
                                           val typeParameters: util.List[ElementName])
    extends GenericTypeSpecialized {

    override def namespace(ns: Namespace): Unit = this.namespace = ns

    override def alias(): Optional[Alias] = Optional.ofNullable(_alias)

  }

  def apply(@Nullable alias: Alias.NsType, namespace: Namespace,
            genericType: MaybeTypeProxy,
            genericTypeArguments: util.List[MaybeTypeProxy],
            typeParameters: util.List[ElementName]): GenericTypeSpecialized =
    new GenericTypeSpecializedImpl(alias, namespace, genericType, genericTypeArguments, typeParameters)
}
