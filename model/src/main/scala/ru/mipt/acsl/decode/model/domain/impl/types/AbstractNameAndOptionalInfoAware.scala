package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, HasName}

/**
  * @author Artem Shein
  */
private[domain] class AbstractNameAndOptionalInfoAware(val name: ElementName, info: ElementInfo)
  extends AbstractOptionalInfoAware(info) with HasName
