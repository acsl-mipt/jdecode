package ru.mipt.acsl.decode.model

import ru.mipt.acsl.modeling.{ErrorLevel, ResolvingMessage}

/**
  * @author Artem Shein
  */
package object proxy {

  type ResolvingResult = Seq[ResolvingMessage]

  implicit class ResolvingResultHelper(result: ResolvingResult) {
    def hasError: Boolean = result.exists(_.level == ErrorLevel)
  }

}
