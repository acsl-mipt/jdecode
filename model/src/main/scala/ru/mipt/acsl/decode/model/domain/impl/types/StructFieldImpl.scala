package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private class StructFieldImpl(val name: ElementName, val typeUnit: TypeUnit, info: LocalizedString)
  extends AbstractHasInfo(info) with StructField {
  override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
}
