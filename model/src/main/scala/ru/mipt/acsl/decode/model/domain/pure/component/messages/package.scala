package ru.mipt.acsl.decode.model.domain.pure.component

import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

import scala.collection.immutable

/**
  * Created by metadeus on 30.03.16.
  */
package object messages {
  type MessageParameterPathElement = Either[ElementName, ElementRange]
  type MessageParameterPath = immutable.Seq[MessageParameterPathElement]
}
