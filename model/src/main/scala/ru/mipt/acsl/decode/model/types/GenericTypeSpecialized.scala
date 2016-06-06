package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait GenericTypeSpecialized extends DecodeType {

  def genericTypeProxy: MaybeProxy.TypeProxy

  def genericType: DecodeType = genericTypeProxy.obj

  def genericTypeArgumentsProxy: Seq[MaybeProxy.TypeProxy]

  def genericTypeArguments: Seq[DecodeType] = genericTypeArgumentsProxy.map(_.obj)

  def systemName: String = "@" + hashCode()

  override def toString: String =
    s"${this.getClass}{alias = $alias, namespace = $namespace, genericTypeProxy = $genericTypeProxy," +
      s" genericTypeArgumentsProxy = $genericTypeArgumentsProxy, typeParameters = $typeParameters}"

}

object GenericTypeSpecialized {

  private class GenericTypeSpecializedImpl(val alias: Option[Alias.NsType], var namespace: Namespace,
                                           val genericTypeProxy: MaybeProxy.TypeProxy,
                                           val genericTypeArgumentsProxy: Seq[MaybeProxy.TypeProxy],
                                           val typeParameters: Seq[ElementName])
    extends GenericTypeSpecialized

  def apply(alias: Option[Alias.NsType], namespace: Namespace,
            genericType: MaybeProxy.TypeProxy,
            genericTypeArguments: Seq[MaybeProxy.TypeProxy],
            typeParameters: Seq[ElementName]): GenericTypeSpecialized =
    new GenericTypeSpecializedImpl(alias, namespace, genericType, genericTypeArguments, typeParameters)
}
