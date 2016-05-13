package ru.mipt.acsl.decode.model

import ru.mipt.acsl.modeling.ValidatingMessage

/**
  * @author Artem Shein
  */
object ValidatingResult {
  def empty: ValidatingResult = Seq.empty[ValidatingMessage]
}
