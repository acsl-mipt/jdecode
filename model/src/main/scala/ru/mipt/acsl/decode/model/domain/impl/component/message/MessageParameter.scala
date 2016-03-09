package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.MessageParameterImpl

/**
  * @author Artem Shein
  */
object MessageParameter {
  def apply(value: String, info: Option[String] = None): MessageParameter =
    new MessageParameterImpl(value, info)
}
