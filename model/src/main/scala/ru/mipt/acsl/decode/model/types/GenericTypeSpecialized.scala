package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.Namespace
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.DecodeType

/**
  * @author Artem Shein
  */
trait GenericTypeSpecialized extends DecodeType {

  def genericTypeProxy: MaybeProxy[GenericType]

  def genericType: GenericType = genericTypeProxy.obj

  def genericTypeArgumentsProxy: Seq[Option[MaybeProxy[DecodeType]]]

  def genericTypeArguments: Seq[Option[DecodeType]] = genericTypeArgumentsProxy.map(_.map(_.obj))

  override def toString: String = "GenericTypeSpecialized" + super.toString

}

object GenericTypeSpecialized {

  private class Impl(name: ElementName, namespace: Namespace, info: LocalizedString,
                                           val genericTypeProxy: MaybeProxy[GenericType],
                                           val genericTypeArgumentsProxy: Seq[Option[MaybeProxy[DecodeType]]])
    extends AbstractType(name, namespace, info) with GenericTypeSpecialized

  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            genericType: MaybeProxy[GenericType],
            genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]]): GenericTypeSpecialized =
    new Impl(name, namespace, info, genericType, genericTypeArguments)
}
