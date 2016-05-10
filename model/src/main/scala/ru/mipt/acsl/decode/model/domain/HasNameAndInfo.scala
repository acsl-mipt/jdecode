package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.naming.HasName

/**
  * @author Artem Shein
  */
trait HasNameAndInfo extends HasInfo with HasName
