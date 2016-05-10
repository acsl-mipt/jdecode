package ru.mipt.acsl.decode.model.domain

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait HasInfo {
  def info: immutable.Map[Language, String]
}
