package ru.mipt.acsl.decode.model.domain.proxy

import ru.mipt.acsl.decode.modeling.ErrorLevel
import ru.mipt.acsl.decode.modeling.aliases._
import ru.mipt.acsl.decode.modeling.impl.Message

/**
  * @author Artem Shein
  */
object Result {
  def apply(msgs: ResolvingMessage*): ResolvingResult = msgs

  def error(msg: String): ResolvingResult = Result(Message(ErrorLevel, msg))

  def empty: ResolvingResult = Result()
}
