package ru.mipt.acsl.decode.model.domain
package component
package message

import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
case object TypeMessageParameterPathWalker extends ((DecodeType, MessageParameterPathElement) => DecodeType) {
  private val optionWalker = OptionTypeMessageParameterPathWalker
  override def apply(t: DecodeType, pathElement: MessageParameterPathElement): DecodeType =
    optionWalker(t, pathElement).get
}
