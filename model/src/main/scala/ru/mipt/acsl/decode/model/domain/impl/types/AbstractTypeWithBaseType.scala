package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private abstract class AbstractTypeWithBaseType(name: ElementName, namespace: Namespace,
                                                info: Option[String], var baseType: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info)
