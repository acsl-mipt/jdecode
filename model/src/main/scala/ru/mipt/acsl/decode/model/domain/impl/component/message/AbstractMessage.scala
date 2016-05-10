package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractHasInfo
import ru.mipt.acsl.decode.model.domain.pure.component.message.TmMessage

/**
  * @author Artem Shein
  */
private abstract class AbstractMessage(info: LocalizedString) extends AbstractHasInfo(info) with TmMessage
