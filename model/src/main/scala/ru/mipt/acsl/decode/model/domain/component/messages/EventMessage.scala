package ru.mipt.acsl.decode.model.domain.component.messages

import ru.mipt.acsl.decode.model.domain.HasBaseType
import ru.mipt.acsl.decode.model.domain.component.Parameter

trait EventMessage extends TmMessage with HasBaseType {
  def fields: Seq[Either[MessageParameter, Parameter]]
}