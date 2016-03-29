package ru.mipt.acsl.decode.model.domain.pure.types

import ru.mipt.acsl.decode.model.domain.pure.{HasInfo, NamespaceAware, Referenceable}
import ru.mipt.acsl.decode.model.domain.pure.naming.HasName

/**
  * @author Artem Shein
  */
trait DecodeType extends Referenceable with HasName with HasInfo with NamespaceAware {
  override def toString: String = s"{name: ${name.asMangledString}, info: $info}"
}