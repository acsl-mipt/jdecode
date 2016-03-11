package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.types.{PrimitiveType, TypeKind}

/**
  * @author Artem Shein
  */
object PrimitiveType {
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString, kind: TypeKind.Value,
            bitLength: Long = 0): PrimitiveType = new PrimitiveTypeImpl(name, namespace, info, kind, bitLength)
}
