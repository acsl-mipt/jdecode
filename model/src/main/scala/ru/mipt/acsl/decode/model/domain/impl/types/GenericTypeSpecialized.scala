package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
trait GenericTypeSpecialized extends pure.types.GenericTypeSpecialized with DecodeType {
  def genericTypeProxy: MaybeProxy[GenericType]
  override def genericType: GenericType = genericTypeProxy.obj
  def genericTypeArgumentsProxy: Seq[Option[MaybeProxy[DecodeType]]]
  override def genericTypeArguments: Seq[Option[DecodeType]] = genericTypeArgumentsProxy.map(_.map(_.obj))
}

object GenericTypeSpecialized {
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            genericType: MaybeProxy[GenericType],
            genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]]): GenericTypeSpecialized =
    new GenericTypeSpecializedImpl(name, namespace, info, genericType, genericTypeArguments)
}
