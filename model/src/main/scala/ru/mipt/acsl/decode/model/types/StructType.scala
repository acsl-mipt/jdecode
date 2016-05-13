package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}

import scala.collection.immutable

trait StructType extends DecodeType {
  def fields: Seq[StructField]
}

object StructType {

  private class Impl(name: ElementName, namespace: Namespace, info: LocalizedString,
                     var fields: immutable.Seq[StructField])
    extends AbstractType(name, namespace, info) with StructType {
    override def toString: String =
      s"${this.getClass}{name = $name, namespace = $namespace, info = $info, fields = [${fields.map(_.toString).mkString(", ")}]"
  }

  def apply(name: ElementName, namespace: Namespace, info: LocalizedString, fields: immutable.Seq[StructField]): StructType =
    new Impl(name, namespace, info, fields)
}