package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait HasBaseType {
  def baseType: DecodeType = baseTypeProxy.obj
  def baseTypeProxy: MaybeProxy[DecodeType]
}
