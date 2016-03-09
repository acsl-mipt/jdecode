package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class StructFieldImpl(val name: ElementName, val typeUnit: TypeUnit, info: Option[String])
  extends AbstractOptionalInfoAware(info) with StructField {
  override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
}
