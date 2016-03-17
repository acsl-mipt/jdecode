package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.component.message.ParameterParser
import ru.mipt.acsl.decode.model.domain.pure.MessageParameterToken
import ru.mipt.acsl.decode.model.domain.pure.component.messages.MessageParameter

/**
  * @author Artem Shein
  */
case class ParameterWalker(parameter: MessageParameter)
{
  private var currentIndex = 0
  private val result = new ParameterParser(parameter.value).Parameter.run()
  if (result.isFailure)
    sys.error("parsing fails")
  val tokens = result.get

  def hasNext: Boolean = currentIndex < tokens.size

  def next: MessageParameterToken = { currentIndex += 1; tokens(currentIndex - 1) }
}
