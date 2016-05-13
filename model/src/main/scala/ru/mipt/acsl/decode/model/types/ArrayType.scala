package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.Namespace
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.DecodeType

/**
  * @author Artem Shein
  */
trait ArrayType extends DecodeType with HasBaseType {
  def size: ArraySize
}

object ArrayType {

  private class Impl(name: ElementName, ns: Namespace, info: LocalizedString,
                     baseTypeProxy: MaybeProxy[DecodeType], val size: ArraySize)
    extends AbstractBaseTypedType(name, ns, info, baseTypeProxy) with ArrayType {

    if (name.asMangledString.startsWith("["))
      require(name.asMangledString.endsWith("]"))

  }


  def apply(name: ElementName, ns: Namespace, info: LocalizedString, baseTypeProxy: MaybeProxy[DecodeType],
            size: ArraySize): ArrayType = new Impl(name, ns, info, baseTypeProxy, size)
}
