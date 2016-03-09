package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.HasInfo
import ru.mipt.acsl.decode.model.domain.naming.HasName

trait StructField extends HasName with HasInfo {
  def typeUnit: TypeUnit
}
