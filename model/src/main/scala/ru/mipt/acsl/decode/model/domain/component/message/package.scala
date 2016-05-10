package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.ArraySize

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
