package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.types.{PrimitiveType, TypeKind}

/**
  * @author Artem Shein
  */
private class PrimitiveTypeImpl(name: ElementName, namespace: Namespace, info: ElementInfo,
                                val kind: TypeKind.Value, val bitLength: Long = 0)
  extends AbstractType(name, namespace, info) with PrimitiveType
