package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.naming.{Container, ElementName, Namespace}

trait StructType extends DecodeType with Container {

  def objects: Seq[Referenceable]

  def fields: Seq[StructField] = objects.flatMap {
    case f: StructField => Seq(f)
    case _ => Seq.empty
  }

  def field(name: ElementName): Option[StructField] = objects.flatMap {
    case a: Alias.StructField if a.name == name => Seq(a.obj)
  } match {
    case s if s.size == 1 => Some(s.head)
  }

  def systemName: String = "@" + hashCode()

  override def toString: String =
    s"${this.getClass}{alias = $alias, namespace = $namespace, objects = [${objects.map(_.toString).mkString(", ")}]"

}

object StructType {

  private class StructTypeImpl(val alias: Option[Alias.NsType], var namespace: Namespace,
                               var objects: Seq[Referenceable], val typeParameters: Seq[ElementName])
    extends StructType

  def apply(alias: Option[Alias.NsType], namespace: Namespace, objects: Seq[Referenceable],
            typeParameters: Seq[ElementName]): StructType =
    new StructTypeImpl(alias, namespace, objects, typeParameters)
}