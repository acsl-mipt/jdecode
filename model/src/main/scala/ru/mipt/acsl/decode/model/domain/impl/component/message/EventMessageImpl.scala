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
private class EventMessageImpl(component: Component, name: ElementName, id: Option[Int], info: ElementInfo,
                               val fields: Seq[Either[MessageParameter, Parameter]], val baseType: MaybeProxy[DecodeType])
  extends AbstractImmutableMessage(component, name, id, info) with EventMessage
