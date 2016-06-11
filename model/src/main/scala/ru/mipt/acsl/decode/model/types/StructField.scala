package ru.mipt.acsl.decode.model.types

import java.util

import ru.mipt.acsl.decode.model.{Referenceable, ReferenceableVisitor}
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName}
import ru.mipt.acsl.decode.model.registry.Language

/**
  * Created by metadeus on 11.05.16.
  */
trait StructField extends Referenceable with HasName {

  def alias: Alias.StructField

  def typeMeasure: TypeMeasure

  override def name: ElementName = alias.name

  def info: util.Map[Language, String] = alias.info

  def accept[T](visitor: ReferenceableVisitor[T]): T = {
    visitor.visit(this)
  }

}

object StructField {

  private class StructFieldImpl(val alias: Alias.StructField, val typeMeasure: TypeMeasure) extends StructField {

    override def toString: String = s"${this.getClass.getSimpleName}{alias = $alias, typeUnit = $typeMeasure}"

  }

  def apply(alias: Alias.StructField, typeUnit: TypeMeasure): StructField =
    new StructFieldImpl(alias, typeUnit)
}
