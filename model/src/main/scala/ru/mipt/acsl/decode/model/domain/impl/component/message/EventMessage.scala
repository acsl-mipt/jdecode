package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.{Component, Parameter}
import ru.mipt.acsl.decode.model.domain.component.messages.{EventMessage, MessageParameter}
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
object EventMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: ElementInfo,
            fields: Seq[Either[MessageParameter, Parameter]], baseType: MaybeProxy[DecodeType]): EventMessage =
    new EventMessageImpl(component, name, id, info, fields, baseType)
}
