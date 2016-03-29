package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.messages.MessageParameter

/**
  * @author Artem Shein
  */
private class MessageParameterImpl(val value: String, val info: LocalizedString = LocalizedString.empty)
  extends MessageParameter
