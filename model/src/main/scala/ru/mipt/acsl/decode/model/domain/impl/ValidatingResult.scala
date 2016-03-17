package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.pure.ValidatingResult
import ru.mipt.acsl.decode.modeling.aliases.ValidatingMessage

/**
  * @author Artem Shein
  */
object ValidatingResult {
  def empty: ValidatingResult = Seq.empty[ValidatingMessage]
}
