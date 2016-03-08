package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit

trait TypeUnit {
  def t: MaybeProxy[DecodeType]

  def unit: Option[MaybeProxy[DecodeUnit]]
}
