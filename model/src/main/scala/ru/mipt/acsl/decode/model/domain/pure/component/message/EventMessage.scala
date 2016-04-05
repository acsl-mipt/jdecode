package ru.mipt.acsl.decode.model.domain.pure.component.message

import ru.mipt.acsl.decode.model.domain.pure.Parameter
import ru.mipt.acsl.decode.model.domain.pure.types.HasBaseType

trait EventMessage extends TmMessage with HasBaseType {
  def fields: Seq[Either[MessageParameter, Parameter]]
}