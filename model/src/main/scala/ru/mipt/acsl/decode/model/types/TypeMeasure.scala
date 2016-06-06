package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.registry.Measure

/**
  * @author Artem Shein
  */
trait TypeMeasure extends DecodeType {

  def typeProxy: MaybeProxy.TypeProxy

  def t: DecodeType = typeProxy.obj

  def measureProxy: Option[MaybeProxy.Measure]

  def measure: Option[Measure] = measureProxy.map(_.obj)

  override def namespace: Namespace = t.namespace

  override def namespace_=(ns: Namespace): Unit = t.namespace = ns

  override def alias: Option[Alias.Type] = t.alias

  override def systemName: String = t.systemName

  override def typeParameters: Seq[ElementName] = t.typeParameters

  override def toString: String =
    s"${this.getClass}{alias = $alias, namespace = $namespace, t = $t, measure = $measure}"

}

object TypeMeasure {

  private case class TypeMeasureImpl(typeProxy: MaybeProxy.TypeProxy, measureProxy: Option[MaybeProxy.Measure])
    extends TypeMeasure

  def apply(typeProxy: MaybeProxy.TypeProxy, unitProxy: Option[MaybeProxy.Measure]): TypeMeasure =
    TypeMeasureImpl(typeProxy, unitProxy)
}
