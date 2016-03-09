package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.HasNameAndInfo
import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
private abstract class AbstractHasNameAndInfo(val name: ElementName, info: ElementInfo)
  extends AbstractOptionalInfoAware(info) with HasNameAndInfo
