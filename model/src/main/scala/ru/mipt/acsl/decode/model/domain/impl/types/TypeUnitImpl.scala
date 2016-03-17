package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.registry.DecodeUnit

/**
  * @author Artem Shein
  */
private class TypeUnitImpl(val typeProxy: MaybeProxy[DecodeType], val unitProxy: Option[MaybeProxy[DecodeUnit]])
  extends TypeUnit
