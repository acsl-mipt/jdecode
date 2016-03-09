package ru.mipt.acsl.decode.model.domain.impl.component.message

/**
  * @author Artem Shein
  */
private class StatusMessageImpl(component: Component, name: ElementName, id: Option[Int], info: Option[String],
                                val parameters: Seq[MessageParameter], val priority: Option[Int] = None)
  extends AbstractImmutableMessage(component, name, id, info) with StatusMessage
