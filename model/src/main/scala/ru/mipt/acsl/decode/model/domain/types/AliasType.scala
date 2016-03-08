package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.{HasBaseType, HasName}

trait AliasType extends DecodeType with HasName with HasBaseType
