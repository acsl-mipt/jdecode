package ru.mipt.acsl.decode.model.domain.component.messages

import ru.mipt.acsl.decode.model.domain.component.Parameter
import ru.mipt.acsl.decode.model.domain.types.HasBaseType

trait EventMessage extends TmMessage with HasBaseType {
  def fields: Seq[Either[MessageParameter, Parameter]]
}