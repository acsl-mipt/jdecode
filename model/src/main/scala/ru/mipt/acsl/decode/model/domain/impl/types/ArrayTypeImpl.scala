package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.{ArraySize, DecodeType}

/**
  * @author Artem Shein
  */
private class ArrayTypeImpl(name: ElementName, ns: Namespace, info: LocalizedString,
                            baseTypeProxy: MaybeProxy[DecodeType], val size: ArraySize)
  extends AbstractBaseTypedType(name, ns, info, baseTypeProxy) with ArrayType {

  if(name.asMangledString.startsWith("["))
    require(name.asMangledString.endsWith("]"))

}
