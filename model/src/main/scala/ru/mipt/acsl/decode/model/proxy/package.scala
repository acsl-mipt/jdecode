package ru.mipt.acsl.decode.model

import ru.mipt.acsl.modeling.{ErrorLevel, ResolvingMessage}

/**
  * @author Artem Shein
  */
package object proxy {

  type ResolvingMessages = Seq[ResolvingMessage]

  object ResolvingMessages {

    def apply(messages: ResolvingMessage*): ResolvingMessages = messages.toSeq

    def empty: ResolvingMessages = Seq.empty

  }

  implicit class ResolvingResultHelper(result: ResolvingMessages) {
    def hasError: Boolean = result.exists(_.level == ErrorLevel)
  }

}
