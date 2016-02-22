package ru.mipt.acsl.decode.modeling.impl

import ru.mipt.acsl.decode.modeling._
import ru.mipt.acsl.decode.modeling.aliases.TransformationMessage

/**
  * @author Artem Shein
  */
case class Message(level: Level, text: String) extends TransformationMessage {

  def error(msg: String): TransformationMessage = message(ErrorLevel, msg)

  def warning(msg: String): TransformationMessage = message(Warning, msg)

  def notice(msg: String): TransformationMessage = message(Notice, msg)

  def message(level: Level, msg: String): TransformationMessage = Message(level, msg)
}
