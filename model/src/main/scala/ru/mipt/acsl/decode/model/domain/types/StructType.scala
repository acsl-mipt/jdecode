package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.ElementName

import scala.collection.immutable

trait StructType extends DecodeType {
  def fields: Seq[StructField]
}

object StructType {
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString, fields: immutable.Seq[StructField]): StructType =
    new StructTypeImpl(name, namespace, info, fields)
}