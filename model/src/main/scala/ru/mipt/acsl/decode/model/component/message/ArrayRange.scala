package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.component.message
import ru.mipt.acsl.decode.model.types.ArraySize

/**
  * Created by metadeus on 05.04.16.
  */
trait ArrayRange {
  def min: BigInt
  def max: Option[BigInt]
  def size(arraySize: ArraySize): ArraySize
}

object ArrayRange {

  private val Zero = BigInt(0)

  private case class ArrayRangeImpl(min: BigInt, max: Option[BigInt]) extends ArrayRange {

    override def toString: String = (min, max) match {
      case (Zero, None) => "*"
      case (_, None) => min + "..*"
      case _ => min + ".." + max.get
    }

    override def size(arraySize: ArraySize): ArraySize = (min, max) match {
      case (Zero, None) => arraySize
      case (_, None) => (arraySize.min, arraySize.max) match {
        case (Zero, Zero) => ArraySize(0, arraySize.max)
        case (_, Zero) => ArraySize(arraySize.min, arraySize.max - min + 1)
          ArraySize(min, arraySize.max - min + 1)
      }
      case _ =>
        val exact = max.get - min + 1
        ArraySize(exact, exact)
    }
  }

  def apply(min: BigInt, max: Option[BigInt]): message.ArrayRange = ArrayRangeImpl(min, max)
}
