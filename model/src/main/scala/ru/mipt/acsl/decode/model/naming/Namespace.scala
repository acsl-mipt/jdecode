package ru.mipt.acsl.decode.model.naming

import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.registry.Measure
import ru.mipt.acsl.decode.model.types.DecodeType
import ru.mipt.acsl.decode.model.{HasNameAndInfo, LocalizedString, Referenceable}

import scala.collection.immutable

/**
  * @author Artem Shein
  */

/**
  * Mutable Namespace
  */
trait Namespace extends Referenceable with HasNameAndInfo {

  /**
    * Get units of current namespace
    *
    * @return Seq of [[Measure]]
    */
  def measures: immutable.Seq[Measure]

  /**
    * Get types of current namespace
    *
    * @return Seq of [[DecodeType]]
    */
  def types: immutable.Seq[DecodeType]

  /**
    * Get subset of current namespaces
    *
    * @return Seq of [[Namespace]]
    */
  def subNamespaces: immutable.Seq[Namespace]

  /**
    * Get parent [[Namespace]] of current namespace
    *
    * @return Parent [[Namespace]] if it define, otherwise - None
    */
  def parent: Option[Namespace]

  /**
    * Get components of current namespace
    *
    * @return Seq of [[Component]]
    */
  def components: immutable.Seq[Component]

  def parent_=(ns: Option[Namespace]): Unit
  def subNamespaces_=(nses: immutable.Seq[Namespace]): Unit

  def measures_=(u: immutable.Seq[Measure]): Unit
  def types_=(t: immutable.Seq[DecodeType]): Unit
  def components_=(c: immutable.Seq[Component]): Unit
}

object Namespace {

  private class Impl(var name: ElementName, var info: LocalizedString, var parent: Option[Namespace],
                     var types: immutable.Seq[DecodeType], var measures: immutable.Seq[Measure],
                     var subNamespaces: immutable.Seq[Namespace], var components: immutable.Seq[Component])
    extends Namespace

  def apply(name: ElementName, info: LocalizedString = LocalizedString.empty, parent: Option[Namespace] = None,
            types: immutable.Seq[DecodeType] = immutable.Seq.empty,
            units: immutable.Seq[Measure] = immutable.Seq.empty,
            subNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty,
            components: immutable.Seq[Component] = immutable.Seq.empty): Namespace =
    new Impl(name, info, parent, types, units, subNamespaces, components)
}
