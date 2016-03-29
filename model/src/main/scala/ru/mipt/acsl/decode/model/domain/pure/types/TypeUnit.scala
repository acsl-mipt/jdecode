package ru.mipt.acsl.decode.model.domain.pure.types

import ru.mipt.acsl.decode.model.domain.pure.registry.DecodeUnit

/**
  * @author Artem Shein
  */
trait TypeUnit {
  def t: DecodeType

  def unit: Option[DecodeUnit]
}
