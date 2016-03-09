package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object AliasType {
  def apply(name: ElementName, namespace: Namespace, baseType: MaybeProxy[DecodeType],
            info: Option[String]): AliasType = new AliasTypeImpl(name, namespace, baseType, info)
}
