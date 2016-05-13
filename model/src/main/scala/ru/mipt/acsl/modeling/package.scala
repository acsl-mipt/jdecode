package ru.mipt.acsl

/**
  * @author Artem Shein
  */
package object modeling {
  type ResolvingMessage = ModelingMessage
  type TransformationMessage = ModelingMessage
  type ValidatingMessage = ModelingMessage

  sealed abstract class Level(val name: String)
  case object Notice extends Level("notice")
  case object Warning extends Level("warning")
  case object ErrorLevel extends Level("error")
}
