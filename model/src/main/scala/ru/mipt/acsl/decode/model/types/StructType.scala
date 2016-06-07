package ru.mipt.acsl.decode.model.types

import java.util
import scala.collection.JavaConversions._
import org.jetbrains.annotations.Nullable
import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.naming.{Container, ElementName, Namespace}

trait StructType extends DecodeType with Container {

  def objects: util.List[Referenceable]

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

  private class StructTypeImpl(@Nullable val alias: Alias.NsType, var namespace: Namespace,
                               var objects: util.List[Referenceable], val typeParameters: util.List[ElementName])
    extends StructType {

    override def objects(objects: util.List[Referenceable]): Unit = this.objects = objects

    override def namespace(ns: Namespace): Unit = this.namespace = ns

  }

  def apply(@Nullable alias: Alias.NsType, namespace: Namespace, objects: util.List[Referenceable],
            typeParameters: util.List[ElementName]): StructType =
    new StructTypeImpl(alias, namespace, objects, typeParameters)
}