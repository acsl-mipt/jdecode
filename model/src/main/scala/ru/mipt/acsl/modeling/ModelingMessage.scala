package ru.mipt.acsl.modeling

/**
  * @author Artem Shein
  */
trait ModelingMessage {
  def text: String
  def level: Level
}
