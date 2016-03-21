package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure

/**
  * @author Artem Shein
  */
trait HasBaseType extends pure.types.HasBaseType {
  override def baseType: DecodeType = baseTypeProxy.obj
  def baseTypeProxy: MaybeProxy[DecodeType]
}
