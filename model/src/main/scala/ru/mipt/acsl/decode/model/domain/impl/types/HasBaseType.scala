package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait HasBaseType extends pure.types.HasBaseType {
  override def baseType: DecodeType = baseTypeProxy.obj
  def baseTypeProxy: MaybeProxy[DecodeType]
}
