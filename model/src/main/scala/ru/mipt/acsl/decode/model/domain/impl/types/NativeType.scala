package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.types.NativeType

/**
  * @author Artem Shein
  */
object NativeType {
  def apply(name: ElementName, ns: Namespace, info: ElementInfo): NativeType = new NativeTypeImpl(name, ns, info)
}
