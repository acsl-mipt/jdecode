package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.{StructField, TypeUnit}

/**
  * @author Artem Shein
  */
private class StructFieldImpl(val name: ElementName, val typeUnit: TypeUnit, info: LocalizedString)
  extends AbstractOptionalInfoAware(info) with StructField {
  override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
}
