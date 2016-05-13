package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.types.{DecodeType, HasBaseType, Parameter}
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

trait EventMessage extends TmMessage with HasBaseType {

  def fields: Seq[Either[MessageParameter, Parameter]]

  def baseTypeProxy: MaybeProxy[DecodeType]

  override def baseType: DecodeType = baseTypeProxy.obj

}

object EventMessage {

  private class EventMessageImpl(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
                                 val fields: Seq[Either[MessageParameter, Parameter]],
                                 val baseTypeProxy: MaybeProxy[DecodeType])
    extends AbstractImmutableMessage(component, name, id, info) with EventMessage

  def apply(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
            fields: Seq[Either[MessageParameter, Parameter]],
            baseTypeProxy: MaybeProxy[DecodeType]): EventMessage =
    new EventMessageImpl(component, name, id, info, fields, baseTypeProxy)
}
