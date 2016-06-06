package ru.mipt.acsl.decode.model.registry

import ru.mipt.acsl.decode.model.naming.{ElementName, HasName, Namespace}
import ru.mipt.acsl.decode.model.types.Alias
import ru.mipt.acsl.decode.model.{HasInfo, HasNamespace, LocalizedString, Referenceable}

/**
  * @author Artem Shein
  */
trait Measure extends Referenceable with HasNamespace with HasName with HasInfo {

  def alias: Alias.NsMeasure

  def display: LocalizedString

  override def name: ElementName = alias.name

  override def namespace: Namespace = alias.parent

  override def info: LocalizedString = alias.info

}

object Measure {

  private case class MeasureImpl(alias: Alias.NsMeasure, var display: LocalizedString)
    extends Measure

  def apply(alias: Alias.NsMeasure,
            display: LocalizedString = LocalizedString.empty): Measure =
    MeasureImpl(alias, display)
}

