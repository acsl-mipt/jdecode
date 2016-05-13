package ru.mipt.acsl.decode.model.types

/**
  * @author Artem Shein
  */
trait ArraySize {
  def min: Long
  def max: Long
}

object ArraySize {

  private case class Impl(min: Long = 0, max: Long = 0) extends ArraySize {
    require(min >= 0)
    require(max >= 0)

    override def toString: String = (min, max) match {
      case (0, 0) => "*"
      case (_, 0) => min + "..*"
      case (a, b) if a == b => min.toString
      case _ => min + ".." + max
    }

  }

  def apply(min: Long = 0, max: Long = 0): ArraySize = Impl(min, max)
}
