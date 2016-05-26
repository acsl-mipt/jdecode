package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.{HasInfo, NamespaceAware, Referenceable}
import ru.mipt.acsl.decode.model.naming.{Fqn, HasName, Namespace}
import ru.mipt.acsl.decode.model.naming.{HasName, Namespace}

/**
  * @author Artem Shein
  */
trait DecodeType extends Referenceable with HasName with HasInfo with NamespaceAware {

  import DecodeType._

  override def namespace: Namespace

  def namespace_=(ns: Namespace): Unit

  override def toString: String = s"{name: ${name.asMangledString}, info: $info}"

  def fqn: Fqn = Fqn(namespace.fqn.parts :+ name)

  def isUnit: Boolean = fqn == UnitFqn // fixme: subtyping & alias

  def isArray: Boolean = fqn == ArrayFqn // fixme: subtyping & alias

}

object DecodeType {

  val UnitFqn = Fqn.newFromSource("decode.unit")

  val ArrayFqn = Fqn.newFromSource("decode.array")

}