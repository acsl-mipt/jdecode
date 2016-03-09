package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object GenericTypeSpecialized {
  def apply(name: ElementName, namespace: Namespace, info: Option[String],
            genericType: MaybeProxy[GenericType],
            genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]]): GenericTypeSpecialized =
    new GenericTypeSpecializedImpl(name, namespace, info, genericType, genericTypeArguments)
}
