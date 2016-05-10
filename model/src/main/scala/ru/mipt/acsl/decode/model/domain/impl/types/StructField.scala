package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.{LocalizedString, pure}
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
trait StructField extends pure.types.StructField {
  override def typeUnit: TypeUnit
}

object StructField {
  def apply(name: ElementName, typeUnit: TypeUnit, info: LocalizedString): StructField =
    new StructFieldImpl(name, typeUnit, info)
}
