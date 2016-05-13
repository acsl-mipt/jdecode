package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.{HasInfo, NamespaceAware, Referenceable}
import ru.mipt.acsl.decode.model.naming.{HasName, Namespace}
import ru.mipt.acsl.decode.model.{HasInfo, NamespaceAware, Referenceable}
import ru.mipt.acsl.decode.model.naming.{HasName, Namespace}

/**
  * @author Artem Shein
  */
trait DecodeType extends Referenceable with HasName with HasInfo with NamespaceAware {

  override def namespace: Namespace

  def namespace_=(ns: Namespace): Unit

  override def toString: String = s"{name: ${name.asMangledString}, info: $info}"

}
