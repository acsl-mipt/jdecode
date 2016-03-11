package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.HasInfo
import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString

/**
  * @author Artem Shein
  */
private[domain] abstract class AbstractOptionalInfoAware(val info: LocalizedString) extends HasInfo
