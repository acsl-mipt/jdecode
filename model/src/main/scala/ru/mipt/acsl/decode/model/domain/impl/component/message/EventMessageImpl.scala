package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.types.{DecodeType, Parameter}
import ru.mipt.acsl.decode.model.domain.pure.component.message.MessageParameter
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString

/**
  * @author Artem Shein
  */
private class EventMessageImpl(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
                               val fields: Seq[Either[MessageParameter, Parameter]],
                               val baseTypeProxy: MaybeProxy[DecodeType])
  extends AbstractImmutableMessage(component, name, id, info) with EventMessage
