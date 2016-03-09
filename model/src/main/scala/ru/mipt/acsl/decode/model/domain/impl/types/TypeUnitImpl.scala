package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, TypeUnit}

/**
  * @author Artem Shein
  */
private class TypeUnitImpl(val t: MaybeProxy[DecodeType], val unit: Option[MaybeProxy[DecodeUnit]])
  extends TypeUnit
