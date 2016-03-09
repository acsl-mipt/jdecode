package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.MessageParameterRefWalker

/**
  * @author Artem Shein
  */
private class MessageParameterImpl(val value: String, val info: Option[String] = None) extends MessageParameter {
  def ref(component: Component): MessageParameterRef =
    new MessageParameterRefWalker(component, None, tokens)

  private def tokens: Seq[MessageParameterToken] = ParameterWalker(this).tokens
}
