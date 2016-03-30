package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.types.DecodeType
import ru.mipt.acsl.decode.model.domain.pure.component.messages.MessageParameterPathElement

/**
  * @author Artem Shein
  */
case object TypeMessageParameterPathWalker extends ((DecodeType, MessageParameterPathElement) => DecodeType) {
  private val optionWalker = OptionTypeMessageParameterPathWalker
  override def apply(t: DecodeType, pathElement: MessageParameterPathElement): DecodeType =
    optionWalker(t, pathElement).get
}
