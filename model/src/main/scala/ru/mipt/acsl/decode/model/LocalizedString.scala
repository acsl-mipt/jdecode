package ru.mipt.acsl.decode.model

import ru.mipt.acsl.decode.model.registry.Language

import scala.collection.immutable

/**
  * @author Artem Shein
  */
object LocalizedString {
  def empty: LocalizedString = immutable.Map.empty[Language, String]
}
