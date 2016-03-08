package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.DecodeUnit
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy

trait TypeUnit {
  def t: MaybeProxy[DecodeType]

  def unit: Option[MaybeProxy[DecodeUnit]]
}
