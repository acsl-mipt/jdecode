package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.types.{StructField, StructType}

/**
  * @author Artem Shein
  */
object StructType {
  def apply(name: ElementName, namespace: Namespace, info: ElementInfo, fields: Seq[StructField]): StructType =
    new StructTypeImpl(name, namespace, info, fields)
}
