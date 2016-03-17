package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.types.AbstractHasInfo
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.component.messages.TmMessage

/**
  * @author Artem Shein
  */
private abstract class AbstractMessage(info: LocalizedString) extends AbstractHasInfo(info) with TmMessage
