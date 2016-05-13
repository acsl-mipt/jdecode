package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.modeling.{ErrorLevel, Message, ResolvingMessage}

/**
  * @author Artem Shein
  */
object Result {
  def apply(msgs: ResolvingMessage*): ResolvingResult = msgs

  def error(msg: String): ResolvingResult = Result(Message(ErrorLevel, msg))

  def empty: ResolvingResult = Result()
}
