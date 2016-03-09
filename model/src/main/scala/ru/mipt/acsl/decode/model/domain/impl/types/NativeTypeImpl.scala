package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class NativeTypeImpl(name: ElementName, ns: Namespace, info: Option[String]) extends AbstractType(name, ns, info) with NativeType
