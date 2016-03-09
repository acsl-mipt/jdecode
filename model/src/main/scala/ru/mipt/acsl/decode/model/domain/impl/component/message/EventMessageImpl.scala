package ru.mipt.acsl.decode.model.domain.impl.component.message

/**
  * @author Artem Shein
  */
private class EventMessageImpl(component: Component, name: ElementName, id: Option[Int], info: Option[String],
                               val fields: Seq[Either[MessageParameter, Parameter]], val baseType: MaybeProxy[DecodeType])
  extends AbstractImmutableMessage(component, name, id, info) with EventMessage
