package ru.mipt.acsl.decode.model.domain.pure.component.message

import ru.mipt.acsl.decode.model.domain.pure.types.ArraySize

/**
  * Created by metadeus on 05.04.16.
  */
trait ArrayRange {
  def min: Long
  def max: Option[Long]
  def size(arraySize: ArraySize): ArraySize
}
