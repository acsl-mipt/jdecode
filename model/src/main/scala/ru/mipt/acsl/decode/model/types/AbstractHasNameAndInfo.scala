package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.HasName
import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
private abstract class AbstractHasNameAndInfo(val name: ElementName, info: LocalizedString)
  extends AbstractHasInfo(info) with HasName
