package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.NativeType

/**
  * @author Artem Shein
  */
trait PrimitiveType extends NativeType {
  def bitLength: Long

  def kind: TypeKind.Value
}

object PrimitiveType {
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString, kind: TypeKind.Value,
            bitLength: Long = 0): PrimitiveType =
    new PrimitiveTypeImpl(name, namespace, info, kind, bitLength)
}
