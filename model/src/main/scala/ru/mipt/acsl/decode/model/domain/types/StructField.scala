package ru.mipt.acsl.decode.model.domain
package types

import ru.mipt.acsl.decode.model.domain.naming.{ElementName, HasName}

/**
  * Created by metadeus on 11.05.16.
  */
trait StructField extends HasName with HasInfo {
  def typeUnit: TypeUnit
}

object StructField {
  def apply(name: ElementName, typeUnit: TypeUnit, info: LocalizedString): StructField =
    new StructFieldImpl(name, typeUnit, info)
}
