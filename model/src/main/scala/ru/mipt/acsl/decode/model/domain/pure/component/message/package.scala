package ru.mipt.acsl.decode.model.domain.pure.component

import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.ArraySize

import scala.collection.immutable

/**
  * Created by metadeus on 30.03.16.
  */
package object message {
  type MessageParameterPathElement = Either[ElementName, ArrayRange]
  type MessageParameterPath = immutable.Seq[MessageParameterPathElement]
}
