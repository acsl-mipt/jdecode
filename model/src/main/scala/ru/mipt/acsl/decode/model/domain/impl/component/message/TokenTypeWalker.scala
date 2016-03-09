package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.MessageParameterToken
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
case object TokenTypeWalker extends ((DecodeType, MessageParameterToken) => DecodeType) {
  private val optionWalker = TokenOptionTypeWalker
  override def apply(t: DecodeType, token: MessageParameterToken): DecodeType = optionWalker(t, token).get
}
