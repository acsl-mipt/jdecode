package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.types.{DecodeType, Parameter}
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.HasBaseType
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString

/**
  * @author Artem Shein
  */
trait EventMessage extends pure.component.messages.EventMessage with HasBaseType {
  def baseTypeProxy: MaybeProxy[DecodeType]
  override def baseType: DecodeType = baseTypeProxy.obj
  override def fields: Seq[Either[pure.component.messages.MessageParameter, Parameter]]
}

object EventMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
            fields: Seq[Either[pure.component.messages.MessageParameter, Parameter]],
            baseTypeProxy: MaybeProxy[DecodeType]): EventMessage =
    new EventMessageImpl(component, name, id, info, fields, baseTypeProxy)
}
