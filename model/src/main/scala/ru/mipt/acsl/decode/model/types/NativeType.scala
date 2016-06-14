package ru.mipt.acsl.decode.model.types

import java.util
import java.util.Optional

import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}

/**
  * @author Artem Shein
  */
trait NativeType extends DecodeType {

  def systemName: String = "@" + hashCode()

  override def toString: String =
    s"${this.getClass}{alias = $alias, namespace = $namespace, typeParameters = $typeParameters}"

}

object NativeType {

  private class NativeTypeImpl(val _alias: Alias.NsType, var namespace: Namespace,
                               val typeParameters: util.List[ElementName])
    extends NativeType {

    override def namespace(ns: Namespace): Unit = this.namespace = ns

    override def alias(): Optional[Alias] = Optional.ofNullable(_alias)
  }

  def apply(alias: Alias.NsType, ns: Namespace, typeParameters: util.List[ElementName]): NativeType =
    new NativeTypeImpl(alias, ns, typeParameters)
}
