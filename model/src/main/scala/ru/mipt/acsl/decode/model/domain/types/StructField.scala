package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.{HasName, HasOptionInfo}

trait StructField extends HasName with HasOptionInfo {
  def typeUnit: TypeUnit
}
