package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object ArrayType {
  def apply(name: ElementName, ns: Namespace, info: Option[String], baseType: MaybeProxy[DecodeType],
            size: ArraySize): ArrayType = new ArrayTypeImpl(name, ns, info, baseType, size)
}
