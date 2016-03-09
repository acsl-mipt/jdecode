package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class SubTypeImpl(name: ElementName, namespace: Namespace, info: Option[String],
                          val baseType: MaybeProxy[DecodeType], val range: Option[SubTypeRange])
  extends AbstractType(name, namespace, info) with SubType
