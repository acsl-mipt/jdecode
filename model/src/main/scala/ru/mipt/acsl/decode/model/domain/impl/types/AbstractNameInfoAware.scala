package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.{ElementName, HasName}

/**
  * @author Artem Shein
  */
private[domain] class AbstractNameInfoAware(val name: ElementName, info: LocalizedString)
  extends AbstractHasInfo(info) with HasName
