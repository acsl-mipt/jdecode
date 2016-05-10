package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractHasInfo
import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
private[domain] class StructFieldImpl(val name: ElementName, val typeUnit: TypeUnit, info: LocalizedString)
  extends AbstractHasInfo(info) with StructField {
  override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
}
