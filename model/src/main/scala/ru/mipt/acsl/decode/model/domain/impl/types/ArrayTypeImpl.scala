package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class ArrayTypeImpl(name: ElementName, ns: Namespace, info: Option[String],
                            val baseType: MaybeProxy[DecodeType], val size: ArraySize)
  extends AbstractType(name, ns, info) with ArrayType
