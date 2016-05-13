package ru.mipt.acsl.decode.model.component

import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.types.ArraySize

import scala.collection.immutable

/**
  * Created by metadeus on 30.03.16.
  */
package object message {
  type MessageParameterPathElement = Either[ElementName, ArrayRange]
  type MessageParameterPath = immutable.Seq[MessageParameterPathElement]

  implicit class MessageParameterHelper(val mp: MessageParameter) {

    def ref(component: Component): MessageParameterRef =
      new MessageParameterRefWalker(component, None, mp.path)

  }
}
