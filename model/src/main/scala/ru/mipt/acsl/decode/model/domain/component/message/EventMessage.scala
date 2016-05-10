package ru.mipt.acsl.decode.model.domain.component.message

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.impl.types.Parameter
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, HasBaseType}

trait EventMessage extends TmMessage with HasBaseType {

  def fields: Seq[Either[MessageParameter, Parameter]]

  def baseTypeProxy: MaybeProxy[DecodeType]

  override def baseType: DecodeType = baseTypeProxy.obj

}

object EventMessage {
  def apply(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
            fields: Seq[Either[MessageParameter, Parameter]],
            baseTypeProxy: MaybeProxy[DecodeType]): EventMessage =
    new EventMessageImpl(component, name, id, info, fields, baseTypeProxy)
}
