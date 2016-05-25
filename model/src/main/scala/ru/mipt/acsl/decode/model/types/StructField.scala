package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.naming.{ElementName, HasName}
import ru.mipt.acsl.decode.model.{HasInfo, LocalizedString}

/**
  * Created by metadeus on 11.05.16.
  */
trait StructField extends HasName with HasInfo {
  def typeUnit: TypeMeasure
}

object StructField {

  private class Impl(val name: ElementName, val typeUnit: TypeMeasure, info: LocalizedString)
    extends AbstractHasInfo(info) with StructField {
    override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
  }

  def apply(name: ElementName, typeUnit: TypeMeasure, info: LocalizedString): StructField =
    new Impl(name, typeUnit, info)
}