package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.modeling.ErrorLevel
import ru.mipt.acsl.decode.modeling.aliases.ResolvingMessage

/**
  * @author Artem Shein
  */
package object proxy {

  type ResolvingResult = Seq[ResolvingMessage]

  implicit class ResolvingResultHelper(result: ResolvingResult) {
    def hasError: Boolean = result.exists(_.level == ErrorLevel)
  }

}
