package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}

/**
  * @author Artem Shein
  */
trait Const extends DecodeType {

  def value: String

  override def toString: String =
    s"${this.getClass}{alias = $alias, namespace = $namespace, value = $value}"

}

object Const {

  private class ConstImpl(val alias: Option[Alias.NsConst], var namespace: Namespace, val value: String,
                          val typeParameters: Seq[ElementName])
    extends Const {

    def systemName: String = value

  }

  def apply(alias: Option[Alias.NsConst], ns: Namespace, value: String): Const =
    new ConstImpl(alias, ns, value, Seq.empty)

}
