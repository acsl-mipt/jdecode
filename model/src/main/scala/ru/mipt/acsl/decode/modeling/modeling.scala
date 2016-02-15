package ru.mipt.acsl.decode.modeling

sealed abstract class Level(val name: String)
case object Notice extends Level("notice")
case object Warning extends Level("warning")
case object ErrorLevel extends Level("error")

package object aliases {
  type ResolvingMessage = ModelingMessage
  type TransformationMessage = ModelingMessage
}

import aliases._

trait ModelingMessage {
  def text: String
  def level: Level
}

trait TransformationResult[T] {
  def result: Option[T]
  def messages: Seq[TransformationMessage]
  def hasError: Boolean
}
