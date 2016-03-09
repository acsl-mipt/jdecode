package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class AliasTypeImpl(name: ElementName, namespace: Namespace, val baseType: MaybeProxy[DecodeType],
                            info: Option[String])
  extends AbstractType(name, namespace, info) with AliasType with HasBaseType {
  def optionName = Some(name)
}
