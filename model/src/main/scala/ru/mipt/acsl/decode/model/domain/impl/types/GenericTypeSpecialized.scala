package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType

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
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            genericType: MaybeProxy[GenericType],
            genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]]): GenericTypeSpecialized =
    new GenericTypeSpecializedImpl(name, namespace, info, genericType, genericTypeArguments)
}
