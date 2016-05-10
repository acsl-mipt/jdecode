package ru.mipt.acsl.decode.model.domain
package impl.naming

import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.types.DecodeType

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
    * @return Seq of [[DecodeUnit]]
    */
  def units: immutable.Seq[DecodeUnit]

  /**
    * Get types of current namespace
    * @return Seq of [[DecodeType]]
    */
  def types: immutable.Seq[DecodeType]

  /**
    * Get subset of current namespaces
    * @return Seq of [[Namespace]]
    */
  def subNamespaces: immutable.Seq[Namespace]

  /**
    * Get parent [[Namespace]] of current namespace
    * @return Parent [[Namespace]] if it define, otherwise - None
    */
  def parent: Option[Namespace]

  /**
    * Get components of current namespace
    * @return Seq of [[Component]]
    */
  def components: immutable.Seq[Component]

  def parent_=(ns: Option[Namespace]): Unit
  def subNamespaces_=(nses: immutable.Seq[Namespace]): Unit
  def units_=(u: immutable.Seq[DecodeUnit]): Unit
  def types_=(t: immutable.Seq[DecodeType]): Unit
  def components_=(c: immutable.Seq[Component]): Unit
}

object Namespace {
  def apply(name: ElementName, info: LocalizedString = LocalizedString.empty, parent: Option[Namespace] = None,
            types: immutable.Seq[DecodeType] = immutable.Seq.empty,
            units: immutable.Seq[DecodeUnit] = immutable.Seq.empty,
            subNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty,
            components: immutable.Seq[Component] = immutable.Seq.empty): Namespace =
    new NamespaceImpl(name, info, parent, types, units, subNamespaces, components)
}
