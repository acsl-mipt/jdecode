package ru.mipt.acsl.decode.model.registry

import java.util

import ru.mipt.acsl.decode.model.naming.{ElementName, HasName, Namespace}
import ru.mipt.acsl.decode.model.types.Alias
import ru.mipt.acsl.decode.model._

/**
  * @author Artem Shein
  */
trait Measure extends Referenceable with HasAlias {

  override def alias: Alias.NsMeasure

  def display: util.Map[Language, String]

  def name: ElementName = alias.name

  def namespace: Namespace = alias.parent

  def info: util.Map[Language, String] = alias.info

  def accept[T](visitor: ReferenceableVisitor[T]): T = {
    visitor.visit(this)
  }

}

object Measure {

  private case class MeasureImpl(alias: Alias.NsMeasure, var display: util.Map[Language, String])
    extends Measure

  def apply(alias: Alias.NsMeasure,
            display: util.Map[Language, String] = util.Collections.emptyMap()): Measure =
    MeasureImpl(alias, display)
}

