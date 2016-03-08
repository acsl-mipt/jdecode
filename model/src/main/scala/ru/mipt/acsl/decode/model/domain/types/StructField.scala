package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.HasOptionInfo
import ru.mipt.acsl.decode.model.domain.naming.HasName

trait StructField extends HasName with HasOptionInfo {
  def typeUnit: TypeUnit
}
