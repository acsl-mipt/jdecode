package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object NativeType {
  def apply(name: ElementName, ns: Namespace, info: Option[String]): NativeType = new NativeTypeImpl(name, ns, info)
}
