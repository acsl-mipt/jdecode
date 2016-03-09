package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.types.DecodeType
import ru.mipt.acsl.decode.model.domain.HasInfo
import ru.mipt.acsl.decode.model.domain.naming.HasName

trait Parameter extends HasName with HasInfo {
  def unit: Option[MaybeProxy[DecodeUnit]]

  def paramType: MaybeProxy[DecodeType]
}