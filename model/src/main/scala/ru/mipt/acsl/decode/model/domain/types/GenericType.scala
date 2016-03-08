package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.naming.ElementName

trait GenericType extends DecodeType {
  def typeParameters: Seq[Option[ElementName]]
}
