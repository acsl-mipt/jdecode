package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.pure.component.message.MessageParameter

/**
  * @author Artem Shein
  */
package object message {

  implicit class MessageParameterHelper(val mp: MessageParameter) {

    def ref(component: Component): MessageParameterRef =
      new MessageParameterRefWalker(component, None, mp.path)

  }

}
