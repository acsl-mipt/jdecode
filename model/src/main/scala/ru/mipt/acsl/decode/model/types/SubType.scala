package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait SubType extends DecodeType {

  def typeMeasure: TypeMeasure

  def baseTypeProxy: MaybeProxy.TypeProxy = typeMeasure.typeProxy

  def baseType: DecodeType = baseTypeProxy.obj

  def systemName: String = "@" + hashCode()

  override def toString: String =
    s"${this.getClass}{alias = $alias, namespace = $namespace, typeMeasure = $typeMeasure, typeParameters = $typeParameters}"

}

object SubType {

  private class SubTypeImpl(val alias: Option[Alias.NsType], var namespace: Namespace,
                            val typeMeasure: TypeMeasure, val typeParameters: Seq[ElementName])
    extends SubType

  def apply(alias: Option[Alias.NsType], namespace: Namespace, typeMeasure: TypeMeasure,
            typeParameters: Seq[ElementName]): SubType =
    new SubTypeImpl(alias, namespace, typeMeasure, typeParameters)
}
