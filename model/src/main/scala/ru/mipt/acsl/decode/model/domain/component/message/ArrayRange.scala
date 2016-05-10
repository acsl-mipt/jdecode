package ru.mipt.acsl.decode.model.domain.component.message

import ru.mipt.acsl.decode.model.domain.component.message
import ru.mipt.acsl.decode.model.domain.types.ArraySize

/**
  * Created by metadeus on 05.04.16.
  */
trait ArrayRange {
  def min: Long
  def max: Option[Long]
  def size(arraySize: ArraySize): ArraySize
}

object ArrayRange {
  def apply(min: Long, max: Option[Long]): message.ArrayRange = ArrayRangeImpl(min, max)
}
