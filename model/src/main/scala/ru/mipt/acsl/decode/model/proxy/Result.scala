package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.modeling.{ErrorLevel, Message, ResolvingMessage}

/**
  * @author Artem Shein
  */
object Result {
  def apply(msgs: ResolvingMessage*): ResolvingMessages = msgs

  def error(msg: String): ResolvingMessages = Result(Message(ErrorLevel, msg))

  def empty: ResolvingMessages = Result()
}
