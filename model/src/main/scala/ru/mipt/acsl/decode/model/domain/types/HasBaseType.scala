package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy

/**
  * Created by metadeus on 08.03.16.
  */
trait HasBaseType {
  def baseType: MaybeProxy[DecodeType]
}
