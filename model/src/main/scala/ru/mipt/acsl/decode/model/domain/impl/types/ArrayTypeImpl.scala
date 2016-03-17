package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.ArraySize

/**
  * @author Artem Shein
  */
private class ArrayTypeImpl(name: ElementName, ns: Namespace, info: LocalizedString,
                            baseTypeProxy: MaybeProxy[DecodeType], val size: ArraySize)
  extends AbstractBaseTypedType(name, ns, info, baseTypeProxy) with ArrayType
