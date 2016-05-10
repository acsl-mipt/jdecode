package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit

/**
  * @author Artem Shein
  */
private class TypeUnitImpl(val typeProxy: MaybeProxy[DecodeType], val unitProxy: Option[MaybeProxy[DecodeUnit]])
  extends TypeUnit
