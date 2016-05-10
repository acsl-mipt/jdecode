package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.HasInfo
import ru.mipt.acsl.decode.model.domain.pure.naming.HasName


/**
  * @author Artem Shein
  */
trait HasNameAndInfo extends HasInfo with HasName
