package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.{ElementName, HasName}

/**
  * @author Artem Shein
  */
private abstract class AbstractHasNameAndInfo(val name: ElementName, info: LocalizedString)
  extends AbstractHasInfo(info) with HasName