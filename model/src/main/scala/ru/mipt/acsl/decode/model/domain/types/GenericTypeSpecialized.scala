package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy

trait GenericTypeSpecialized extends DecodeType {
  def genericType: MaybeProxy[GenericType]

  def genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]]
}
