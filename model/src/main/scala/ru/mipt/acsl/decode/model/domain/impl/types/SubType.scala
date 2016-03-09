package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object SubType {
  def apply(name: ElementName, namespace: Namespace, info: Option[String],
            baseType: MaybeProxy[DecodeType], range: Option[SubTypeRange]): SubType =
    new SubTypeImpl(name, namespace, info, baseType, range)
}
