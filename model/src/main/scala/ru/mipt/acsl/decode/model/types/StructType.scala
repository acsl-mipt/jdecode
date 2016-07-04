package ru.mipt.acsl.decode.model.types

import java.util
import java.util.Optional

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
    case a: Alias.StructField if a.name == name => Seq(a.obj())
  } match {
    case s if s.size == 1 => Some(s.head)
  }

  def systemName: String = "@" + hashCode()

  override def accept[T](visitor: ReferenceableVisitor[T]): T = {
    visitor.visit(this)
  }

  override def toString: String =
    s"${this.getClass.getSimpleName}{alias = $alias, namespace = $namespace, objects = ${objects.size()} items}"

  override def accept[T](visitor: ContainerVisitor[T]): T = visitor.visit(this)
}

object StructType {

  private class StructTypeImpl(@Nullable val _alias: Alias.NsType, var namespace: Namespace,
                               var objects: util.List[Referenceable], val typeParameters: util.List[ElementName])
    extends StructType {

    override def setObjects(objects: util.List[Referenceable]): Unit = this.objects = objects

    override def alias(): Optional[Alias] = Optional.ofNullable(_alias)

    override def setNamespace(namespace: Namespace): Unit = this.namespace = namespace
  }

  def apply(@Nullable alias: Alias.NsType, namespace: Namespace, objects: util.List[Referenceable],
            typeParameters: util.List[ElementName]): StructType =
    new StructTypeImpl(alias, namespace, objects, typeParameters)
}