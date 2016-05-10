package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.{HasInfo, LocalizedString}

/**
  * @author Artem Shein
  */
private[domain] abstract class AbstractHasInfo(val info: LocalizedString) extends HasInfo
