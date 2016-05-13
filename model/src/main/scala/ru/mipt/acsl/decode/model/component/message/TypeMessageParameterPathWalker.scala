package ru.mipt.acsl.decode.model
package component
package message

import ru.mipt.acsl.decode.model.types.DecodeType

/**
  * @author Artem Shein
  */
case object TypeMessageParameterPathWalker extends ((DecodeType, MessageParameterPathElement) => DecodeType) {
  private val optionWalker = OptionTypeMessageParameterPathWalker
  override def apply(t: DecodeType, pathElement: MessageParameterPathElement): DecodeType =
    optionWalker(t, pathElement).get
}
