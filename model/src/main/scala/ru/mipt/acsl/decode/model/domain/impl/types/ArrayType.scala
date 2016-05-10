package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.{types => t}

/**
  * @author Artem Shein
  */
trait ArrayType extends BaseTypedType with t.ArrayType

object ArrayType {
  def apply(name: ElementName, ns: Namespace, info: LocalizedString, baseTypeProxy: MaybeProxy[DecodeType],
            size: t.ArraySize): ArrayType = new ArrayTypeImpl(name, ns, info, baseTypeProxy, size)
}
