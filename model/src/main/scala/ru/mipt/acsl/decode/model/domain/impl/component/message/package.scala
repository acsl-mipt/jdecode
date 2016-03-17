package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.impl.types.ParameterWalker
import ru.mipt.acsl.decode.model.domain.pure.MessageParameterToken
import ru.mipt.acsl.decode.model.domain.pure.component.messages.MessageParameter

/**
  * @author Artem Shein
  */
package object message {
  implicit class MessageParameterHelper(val mp: MessageParameter) {

    private def tokens: Seq[MessageParameterToken] = ParameterWalker(mp).tokens

    def ref(component: Component): MessageParameterRef =
      new MessageParameterRefWalker(component, None, tokens)
  }
}
