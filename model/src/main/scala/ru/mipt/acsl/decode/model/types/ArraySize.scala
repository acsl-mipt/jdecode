package ru.mipt.acsl.decode.model.types

import scala.math.BigInt

/**
  * @author Artem Shein
  */
trait ArraySize {
  def min: BigInt
  def max: BigInt
}

object ArraySize {

  private val Zero = BigInt(0)

  private case class ArraySizeImpl(min: BigInt = Zero, max: BigInt = Zero) extends ArraySize {

    require(min >= Zero)
    require(max >= Zero)

    override def toString: String = (min, max) match {
      case (Zero, Zero) => "*"
      case (_, Zero) => min + "..*"
      case (a, b) if a == b => min.toString
      case _ => min + ".." + max
    }

  }

  def apply(min: BigInt = Zero, max: BigInt = Zero): ArraySize = ArraySizeImpl(min, max)

  def apply(min: Long, max: Long): ArraySize = apply(min, max)

}
