package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait StructType extends pure.types.StructType with DecodeType {
  def fields: immutable.Seq[StructField]
}

object StructType {
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString, fields: immutable.Seq[StructField]): StructType =
    new StructTypeImpl(name, namespace, info, fields)
}
