package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait GenericTypeSpecialized extends DecodeType {

  def genericTypeProxy: MaybeProxy[GenericType]

  def genericType: GenericType = genericTypeProxy.obj

  def genericTypeArgumentsProxy: Seq[MaybeProxy[DecodeType]]

  def genericTypeArguments: Seq[DecodeType] = genericTypeArgumentsProxy.map(_.obj)

  override def toString: String = "GenericTypeSpecialized" + super.toString

}

object GenericTypeSpecialized {

  private class Impl(name: ElementName, namespace: Namespace, info: LocalizedString,
                                           val genericTypeProxy: MaybeProxy[GenericType],
                                           val genericTypeArgumentsProxy: Seq[MaybeProxy[DecodeType]])
    extends AbstractType(name, namespace, info) with GenericTypeSpecialized

  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            genericType: MaybeProxy[GenericType],
            genericTypeArguments: Seq[MaybeProxy[DecodeType]]): GenericTypeSpecialized =
    new Impl(name, namespace, info, genericType, genericTypeArguments)
}
