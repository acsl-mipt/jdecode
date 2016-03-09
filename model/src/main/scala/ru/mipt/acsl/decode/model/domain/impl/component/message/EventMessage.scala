package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.EventMessageImpl

/**
  * @author Artem Shein
  */
object EventMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: Option[String],
            fields: Seq[Either[MessageParameter, Parameter]], baseType: MaybeProxy[DecodeType]): EventMessage =
    new EventMessageImpl(component, name, id, info, fields, baseType)
}
