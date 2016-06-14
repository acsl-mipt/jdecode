package ru.mipt.acsl.decode.model.component.message

import java.util.Optional

import org.jetbrains.annotations.Nullable
import ru.mipt.acsl.decode.model.component.message
import ru.mipt.acsl.decode.model.types.ArraySize

/**
  * Created by metadeus on 05.04.16.
  */
trait ArrayRange {
  def min: BigInt
  def max: Optional[BigInt]
  def size(arraySize: ArraySize): ArraySize
}

object ArrayRange {

  private val Zero = BigInt(0)

  private case class ArrayRangeImpl(min: BigInt, @Nullable _max: BigInt) extends ArrayRange {

    override def max: Optional[BigInt] = Optional.ofNullable(_max)

    override def toString: String = (min, _max) match {
      case (Zero, null) => "*"
      case (_, null) => min + "..*"
      case _ => min + ".." + max
    }

    override def size(arraySize: ArraySize): ArraySize = (min, _max) match {
      case (Zero, null) => arraySize
      case (_, null) => (arraySize.min, arraySize.max) match {
        case (Zero, Zero) => ArraySize(0, arraySize.max)
        case (_, Zero) => ArraySize(arraySize.min, arraySize.max - min + 1)
          ArraySize(min, arraySize.max - min + 1)
      }
      case _ =>
        val exact = _max - min + 1
        ArraySize(exact, exact)
    }
  }

  def apply(min: BigInt, @Nullable max: BigInt): message.ArrayRange = ArrayRangeImpl(min, max)
}
