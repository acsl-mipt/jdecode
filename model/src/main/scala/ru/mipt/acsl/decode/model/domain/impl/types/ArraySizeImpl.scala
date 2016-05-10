package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.types.ArraySize

/**
  * @author Artem Shein
  */
private case class ArraySizeImpl(min: Long = 0, max: Long = 0) extends ArraySize {
  require(min >= 0)
  require(max >= 0)

  override def toString: String = (min, max) match {
    case (0, 0) => "*"
    case (_, 0) => min + "..*"
    case (a, b) if a == b => min.toString
    case _ => min + ".." + max
  }

}
