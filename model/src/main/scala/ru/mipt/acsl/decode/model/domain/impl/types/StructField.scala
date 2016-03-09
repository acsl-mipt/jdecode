package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object StructField {
  def apply(name: ElementName, typeUnit: TypeUnit, info: Option[String]): StructField =
    new StructFieldImpl(name, typeUnit, info)
}
