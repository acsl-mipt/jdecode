package ru.mipt.acsl.decode

import scala.collection.mutable

/**
  * @author Artem Shein
  */
object ScalaUtil {
  def appendToBuffer[T](buffer: mutable.Buffer[T], elem: T) = buffer += elem
}
