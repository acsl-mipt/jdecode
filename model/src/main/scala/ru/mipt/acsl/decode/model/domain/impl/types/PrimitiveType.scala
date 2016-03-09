package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object PrimitiveType {
  def apply(name: ElementName, namespace: Namespace, info: Option[String], kind: TypeKind.Value,
            bitLength: Long = 0): PrimitiveType = new PrimitiveTypeImpl(name, namespace, info, kind, bitLength)
}
