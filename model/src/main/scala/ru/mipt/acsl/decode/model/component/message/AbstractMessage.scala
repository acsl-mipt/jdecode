package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.types.AbstractHasInfo

/**
  * @author Artem Shein
  */
private abstract class AbstractMessage(info: LocalizedString) extends AbstractHasInfo(info) with TmMessage
