package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.component.message
import ru.mipt.acsl.decode.model.types.ArraySize

/**
  * Created by metadeus on 05.04.16.
  */
trait ArrayRange {
  def min: Long
  def max: Option[Long]
  def size(arraySize: ArraySize): ArraySize
}

object ArrayRange {

  private case class ArrayRangeImpl(min: Long, max: Option[Long]) extends message.ArrayRange {

    override def toString: String = (min, max) match {
      case (0, None) => "*"
      case (_, None) => min + "..*"
      case _ => min + ".." + max.get
    }

    override def size(arraySize: ArraySize): ArraySize = (min, max) match {
      case (0, None) => arraySize
      case (_, None) => (arraySize.min, arraySize.max) match {
        case (0, 0) => ArraySize(0, arraySize.max)
        case (_, 0) => ArraySize(arraySize.min, arraySize.max - min + 1)
          ArraySize(min, arraySize.max - min + 1)
      }
      case _ =>
        val exact = max.get - min + 1
        ArraySize(exact, exact)
    }
  }

  def apply(min: Long, max: Option[Long]): message.ArrayRange = ArrayRangeImpl(min, max)
}
