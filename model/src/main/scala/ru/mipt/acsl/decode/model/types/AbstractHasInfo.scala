package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.{HasInfo, LocalizedString}

/**
  * @author Artem Shein
  */
private[model] abstract class AbstractHasInfo(val info: LocalizedString) extends HasInfo
