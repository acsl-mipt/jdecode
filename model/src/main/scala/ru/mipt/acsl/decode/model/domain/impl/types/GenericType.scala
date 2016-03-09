package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object GenericType {
  def apply(name: ElementName, ns: Namespace, info: Option[String],
            typeParameters: Seq[Option[ElementName]]): GenericType =
    new GenericTypeImpl(name, ns, info, typeParameters)
}
