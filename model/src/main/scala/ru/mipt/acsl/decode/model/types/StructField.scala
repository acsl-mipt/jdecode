package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.{LocalizedString, Referenceable}
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName}

/**
  * Created by metadeus on 11.05.16.
  */
trait StructField extends Referenceable with HasName {

  def alias: Alias.StructField

  def typeMeasure: TypeMeasure

  override def name: ElementName = alias.name

  def info: LocalizedString = alias.info

}

object StructField {

  private class StructFieldImpl(val alias: Alias.StructField, val typeMeasure: TypeMeasure) extends StructField {

    override def toString: String = s"${this.getClass.getSimpleName}{alias = $alias, typeUnit = $typeMeasure}"

  }

  def apply(alias: Alias.StructField, typeUnit: TypeMeasure): StructField =
    new StructFieldImpl(alias, typeUnit)
}
