package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, EnumConstant}

/**
  * @author Artem Shein
  */
trait EnumType extends HasBaseType with ru.mipt.acsl.decode.model.domain.types.EnumType {
  override def extendsTypeOption: Option[EnumType] = extendsOrBaseType.left.toOption
  override def baseTypeOption: Option[DecodeType] = extendsOrBaseType.right.toOption
  def extendsOrBaseTypeProxy: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]]
  def extendsOrBaseType: Either[EnumType, DecodeType] = extendsOrBaseTypeProxy.fold(l => Left(l.obj), r => Right(r.obj))
}

object EnumType {
  def apply(name: ElementName, namespace: Namespace,
            extendsOrBaseTypeProxy: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
            info: LocalizedString, constants: Set[EnumConstant], isFinal: Boolean): EnumType =
    new EnumTypeImpl(name, namespace, extendsOrBaseTypeProxy, info, constants, isFinal)
}
