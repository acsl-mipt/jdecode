package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, EnumConstant, EnumType}

/**
  * @author Artem Shein
  */
private class EnumTypeImpl(name: ElementName, namespace: Namespace,
                           var extendsOrBaseType: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
                           info: ElementInfo, var constants: Set[EnumConstant], var isFinal: Boolean)
  extends AbstractType(name, namespace, info) with EnumType {
  override def extendsType: Option[MaybeProxy[EnumType]] = extendsOrBaseType.left.toOption
  def baseTypeOption: Option[MaybeProxy[DecodeType]] = extendsOrBaseType.right.toOption
  override def baseType: MaybeProxy[DecodeType] = extendsOrBaseType.right.getOrElse(extendsType.get.obj.baseType)
}
