package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object EnumType {
  def apply(name: ElementName, namespace: Namespace,
            extendsOrBaseType: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
            info: Option[String], constants: Set[EnumConstant], isFinal: Boolean): EnumType =
    new EnumTypeImpl(name, namespace, extendsOrBaseType, info, constants, isFinal)
}
