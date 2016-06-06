package ru.mipt.acsl.decode.model.component

import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * Created by metadeus on 30.03.16.
  */
package object message {

  type MessageParameterPathElement = Either[ElementName, ArrayRange]
  type MessageParameterPath = Seq[MessageParameterPathElement]

  /*implicit class MessageParameterHelper(val mp: StatusParameter) {

    def ref(component: Component): MessageParameterRef =
      new MessageParameterRefWalker(component, None, mp.path)

  }*/

}
