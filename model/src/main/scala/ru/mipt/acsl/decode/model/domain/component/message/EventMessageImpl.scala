package ru.mipt.acsl.decode.model.domain
package component
package message

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.types.Parameter
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
private[domain] class EventMessageImpl(component: Component, name: ElementName, id: Option[Int], info: LocalizedString,
                               val fields: Seq[Either[MessageParameter, Parameter]],
                               val baseTypeProxy: MaybeProxy[DecodeType])
  extends AbstractImmutableMessage(component, name, id, info) with EventMessage
