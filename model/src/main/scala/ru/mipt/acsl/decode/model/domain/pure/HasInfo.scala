package ru.mipt.acsl.decode.model.domain.pure

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait HasInfo {
  def info: immutable.Map[Language, String]
}
