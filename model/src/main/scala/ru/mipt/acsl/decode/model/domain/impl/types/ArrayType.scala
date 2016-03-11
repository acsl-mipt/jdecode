package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{ArraySize, ArrayType, DecodeType}

/**
  * @author Artem Shein
  */
object ArrayType {
  def apply(name: ElementName, ns: Namespace, info: LocalizedString, baseType: MaybeProxy[DecodeType],
            size: ArraySize): ArrayType = new ArrayTypeImpl(name, ns, info, baseType, size)
}
