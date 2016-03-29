package ru.mipt.acsl.decode.model.domain.pure.types

import ru.mipt.acsl.decode.model.domain.pure.HasInfo
import ru.mipt.acsl.decode.model.domain.pure.naming.HasName

trait StructField extends HasName with HasInfo {
  def typeUnit: TypeUnit
}
