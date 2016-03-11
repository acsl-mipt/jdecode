package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.{StructField, TypeUnit}

/**
  * @author Artem Shein
  */
object StructField {
  def apply(name: ElementName, typeUnit: TypeUnit, info: LocalizedString): StructField =
    new StructFieldImpl(name, typeUnit, info)
}
