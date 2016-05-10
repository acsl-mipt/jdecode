package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.{ArraySize, DecodeType}

/**
  * @author Artem Shein
  */
trait ArrayType extends DecodeType with HasBaseType {
  def size: ArraySize
}

object ArrayType {
  def apply(name: ElementName, ns: Namespace, info: LocalizedString, baseTypeProxy: MaybeProxy[DecodeType],
            size: ArraySize): ArrayType = new ArrayTypeImpl(name, ns, info, baseTypeProxy, size)
}
