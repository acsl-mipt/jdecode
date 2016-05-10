package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.{HasInfo, NamespaceAware, Referenceable}
import ru.mipt.acsl.decode.model.domain.naming.HasName

/**
  * @author Artem Shein
  */
trait DecodeType extends Referenceable with HasName with HasInfo with NamespaceAware {

  override def namespace: Namespace

  def namespace_=(ns: Namespace): Unit

  override def toString: String = s"{name: ${name.asMangledString}, info: $info}"

}
