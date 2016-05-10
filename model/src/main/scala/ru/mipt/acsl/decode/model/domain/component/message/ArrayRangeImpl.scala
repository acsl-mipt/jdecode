package ru.mipt.acsl.decode.model.domain.component.message

import ru.mipt.acsl.decode.model.domain.component.message
import ru.mipt.acsl.decode.model.domain.impl.types.ArraySize
import ru.mipt.acsl.decode.model.domain.types.ArraySize

/**
  * Created by metadeus on 11.05.16.
  */
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