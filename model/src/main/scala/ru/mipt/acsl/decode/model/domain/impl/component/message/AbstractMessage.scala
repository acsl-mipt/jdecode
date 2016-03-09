package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.messages.TmMessage
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractOptionalInfoAware

/**
  * @author Artem Shein
  */
private abstract class AbstractMessage(info: ElementInfo) extends AbstractOptionalInfoAware(info) with TmMessage
