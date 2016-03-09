package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class GenericTypeImpl(name: ElementName, ns: Namespace, info: Option[String],
                              val typeParameters: Seq[Option[ElementName]])
  extends AbstractType(name, ns, info) with GenericType
