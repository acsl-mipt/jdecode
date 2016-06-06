package ru.mipt.acsl.decode.model.types

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
                               val typeParameters: Seq[ElementName])
    extends NativeType {

    override def alias: Option[Alias.NsType] = Some(_alias)

  }

  def apply(alias: Alias.NsType, ns: Namespace, typeParameters: Seq[ElementName]): NativeType =
    new NativeTypeImpl(alias, ns, typeParameters)
}
