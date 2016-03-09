package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class TypeUnitImpl(val t: MaybeProxy[DecodeType], val unit: Option[MaybeProxy[DecodeUnit]])
  extends TypeUnit
