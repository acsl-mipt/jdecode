package ru.mipt.acsl.decode.model.domain.pure

import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.naming.HasName

/**
  * @author Artem Shein
  */
trait HasNameAndInfo extends pure.HasInfo with HasName
