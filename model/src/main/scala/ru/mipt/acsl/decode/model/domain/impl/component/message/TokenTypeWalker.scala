package ru.mipt.acsl.decode.model.domain.impl.component.message

/**
  * @author Artem Shein
  */
case object TokenTypeWalker extends ((DecodeType, MessageParameterToken) => DecodeType) {
  private val optionWalker = TokenOptionTypeWalker
  override def apply(t: DecodeType, token: MessageParameterToken): DecodeType = optionWalker(t, token).get
}
