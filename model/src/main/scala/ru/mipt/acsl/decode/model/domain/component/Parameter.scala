package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType
import ru.mipt.acsl.decode.model.domain.{DecodeUnit, HasName, HasOptionInfo}

trait Parameter extends HasName with HasOptionInfo {
  def unit: Option[MaybeProxy[DecodeUnit]]

  def paramType: MaybeProxy[DecodeType]
}