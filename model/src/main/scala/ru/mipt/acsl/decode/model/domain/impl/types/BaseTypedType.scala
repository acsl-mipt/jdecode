package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait BaseTypedType extends HasBaseType with DecodeType {
  def baseTypeProxy: MaybeProxy[DecodeType]
  override def baseType: DecodeType = baseTypeProxy.obj
}
