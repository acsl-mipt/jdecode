package ru.mipt.acsl.decode.model.domain.pure

import ru.mipt.acsl.decode.model.domain.pure.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.pure.types.DecodeType

/**
  * @author Artem Shein
  */
trait Parameter extends HasNameAndInfo {
  def unit: Option[DecodeUnit]

  def paramType: DecodeType
}
