package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class EnumTypeImpl(name: ElementName, namespace: Namespace,
                           var extendsOrBaseType: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
                           info: Option[String], var constants: Set[EnumConstant], var isFinal: Boolean)
  extends AbstractType(name, namespace, info) with EnumType {
  override def extendsType: Option[MaybeProxy[EnumType]] = extendsOrBaseType.left.toOption
  def baseTypeOption: Option[MaybeProxy[DecodeType]] = extendsOrBaseType.right.toOption
  override def baseType: MaybeProxy[DecodeType] = extendsOrBaseType.right.getOrElse(extendsType.get.obj.baseType)
}
