package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.EnumConstant

/**
  * @author Artem Shein
  */
private class EnumTypeImpl(name: ElementName, namespace: Namespace,
                           var extendsOrBaseTypeProxy: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
                           info: LocalizedString, var constants: Set[EnumConstant], var isFinal: Boolean)
  extends AbstractType(name, namespace, info) with EnumType {
  def extendsTypeProxy: Option[MaybeProxy[EnumType]] = extendsOrBaseTypeProxy.left.toOption
  override def extendsTypeOption: Option[EnumType] = extendsTypeProxy.map(_.obj)
  override def baseTypeProxy: MaybeProxy[DecodeType] =
    extendsOrBaseTypeProxy.right.getOrElse(extendsTypeOption.get.baseTypeProxy)
}
