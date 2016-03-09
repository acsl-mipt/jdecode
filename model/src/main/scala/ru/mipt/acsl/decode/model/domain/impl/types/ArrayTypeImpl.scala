package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{ArraySize, ArrayType, DecodeType}

/**
  * @author Artem Shein
  */
private class ArrayTypeImpl(name: ElementName, ns: Namespace, info: ElementInfo,
                            val baseType: MaybeProxy[DecodeType], val size: ArraySize)
  extends AbstractType(name, ns, info) with ArrayType
