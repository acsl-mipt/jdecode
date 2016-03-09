package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class PrimitiveTypeImpl(name: ElementName, namespace: Namespace, info: Option[String],
                                val kind: TypeKind.Value, val bitLength: Long = 0)
  extends AbstractType(name, namespace, info) with PrimitiveType
