package ru.mipt.acsl.decode.model

import ru.mipt.acsl.decode.model.naming.HasName

/**
  * @author Artem Shein
  */
trait HasNameAndInfo extends HasInfo with HasName
