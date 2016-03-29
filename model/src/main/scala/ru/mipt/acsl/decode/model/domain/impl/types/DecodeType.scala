package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure

/**
  * @author Artem Shein
  */
trait DecodeType extends pure.types.DecodeType {
  override def namespace: Namespace
  def namespace_=(ns: Namespace): Unit
}
