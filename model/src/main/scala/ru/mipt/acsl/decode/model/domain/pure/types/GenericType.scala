package ru.mipt.acsl.decode.model.domain.pure.types

import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

trait GenericType extends DecodeType {
  def typeParameters: Seq[Option[ElementName]]
}
