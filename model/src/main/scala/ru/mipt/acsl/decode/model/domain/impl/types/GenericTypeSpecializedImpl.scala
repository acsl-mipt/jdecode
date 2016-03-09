package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class GenericTypeSpecializedImpl(name: ElementName, namespace: Namespace, info: Option[String],
                                         val genericType: MaybeProxy[GenericType], val genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]])
  extends AbstractType(name, namespace, info) with GenericTypeSpecialized
