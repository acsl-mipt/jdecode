package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.{Language, LocalizedString}

import scala.collection.immutable

/**
  * @author Artem Shein
  */
object LocalizedString {
  def empty: LocalizedString = immutable.Map.empty[Language, String]
}
