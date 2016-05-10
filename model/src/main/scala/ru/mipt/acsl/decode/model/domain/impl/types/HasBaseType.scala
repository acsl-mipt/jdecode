package ru.mipt.acsl.decode.model.domain
package impl.types

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
trait HasBaseType {
  def baseType: DecodeType = baseTypeProxy.obj
  def baseTypeProxy: MaybeProxy[DecodeType]
}
