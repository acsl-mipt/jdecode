package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.{LocalizedString, MessageParameterToken}
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.component.messages.{MessageParameter, MessageParameterRef}
import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.types.ParameterWalker

/**
  * @author Artem Shein
  */
private class MessageParameterImpl(val value: String, val info: LocalizedString = LocalizedString.empty) extends MessageParameter {
  def ref(component: Component): MessageParameterRef =
    new MessageParameterRefWalker(component, None, tokens)

  private def tokens: Seq[MessageParameterToken] = ParameterWalker(this).tokens
}
