package ru.mipt.acsl.decode.model.domain

import scala.collection.immutable

/**
  * @author Artem Shein
  */
object LocalizedString {
  def empty: LocalizedString = immutable.Map.empty[Language, String]
}
