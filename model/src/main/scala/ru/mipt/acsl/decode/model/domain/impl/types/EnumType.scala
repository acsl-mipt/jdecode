package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.EnumConstant

/**
  * @author Artem Shein
  */
trait EnumType extends BaseTypedType with pure.types.EnumType {
  override def extendsType: Option[EnumType] = extendsOrBaseType.left.toOption
  def extendsOrBaseTypeProxy: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]]
  def extendsOrBaseType: Either[EnumType, DecodeType] = extendsOrBaseTypeProxy.fold(l => Left(l.obj), r => Right(r.obj))
}

object EnumType {
  def apply(name: ElementName, namespace: Namespace,
            extendsOrBaseTypeProxy: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
            info: LocalizedString, constants: Set[EnumConstant], isFinal: Boolean): EnumType =
    new EnumTypeImpl(name, namespace, extendsOrBaseTypeProxy, info, constants, isFinal)
}
