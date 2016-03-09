package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.registry.Language

import scala.collection.immutable

/**
  * @author Artem Shein
  */
object ElementInfo {
  def empty: ElementInfo = immutable.Map.empty[Language, String]
}
