package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object StructType {
  def apply(name: ElementName, namespace: Namespace, info: Option[String], fields: Seq[StructField]): StructType =
    new StructTypeImpl(name, namespace, info, fields)
}
