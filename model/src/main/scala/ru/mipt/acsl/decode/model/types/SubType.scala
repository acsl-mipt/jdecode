package ru.mipt.acsl.decode.model.types

import java.util
import java.util.Optional

import org.jetbrains.annotations.Nullable
import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.proxy.MaybeTypeProxy

/**
  * @author Artem Shein
  */
trait SubType extends DecodeType {

  def typeMeasure: TypeMeasure

  def baseTypeProxy: MaybeTypeProxy = typeMeasure.typeProxy

  def baseType: DecodeType = baseTypeProxy.obj

  def systemName: String = "@" + hashCode()

  override def toString: String =
    s"${this.getClass}{alias = $alias, namespace = $namespace, typeMeasure = $typeMeasure, typeParameters = $typeParameters}"

}

object SubType {

  private class SubTypeImpl(@Nullable val _alias: Alias.NsType, var namespace: Namespace,
                            val typeMeasure: TypeMeasure, val typeParameters: util.List[ElementName])
    extends SubType {

    override def alias(): Optional[Alias] = Optional.ofNullable(_alias)

    override def setNamespace(namespace: Namespace): Unit = this.namespace = namespace
  }

  def apply(@Nullable alias: Alias.NsType, namespace: Namespace, typeMeasure: TypeMeasure,
            typeParameters: util.List[ElementName]): SubType =
    new SubTypeImpl(alias, namespace, typeMeasure, typeParameters)
}
