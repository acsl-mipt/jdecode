package ru.mipt.acsl.decode.model

import ru.mipt.acsl.decode.model.registry.Language

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait HasInfo {
  def info: immutable.Map[Language, String]
}
