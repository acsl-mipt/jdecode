package ru.mipt.acsl.decode.model.domain.naming

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.registry.{DecodeUnit, Language}
import ru.mipt.acsl.decode.model.domain.types.DecodeType

import scala.collection.immutable

/**
  * Namespace trait
  */
trait Namespace extends Referenceable with HasName with HasOptionInfo with Resolvable with Validatable {
  /**
    * String representation of current namespace
    * @return String namespace
    */
  def asString: String

  /**
    * Get units of current namespace
    * @return Seq of [[DecodeUnit]]
    */
  def units: immutable.Seq[DecodeUnit]

  def units_=(units: immutable.Seq[DecodeUnit])

  /**
    * Get types of current namespace
    * @return Seq of [[DecodeType]]
    */
  def types: immutable.Seq[DecodeType]

  def types_=(types: immutable.Seq[DecodeType])

  /**
    * Get subset of current namespaces
    * @return Seq of [[Namespace]]
    */
  def subNamespaces: immutable.Seq[Namespace]

  def subNamespaces_=(namespaces: immutable.Seq[Namespace])

  /**
    * Get parent [[Namespace]] of current namespace
    * @return Parent [[Namespace]] if it define, otherwise - None
    */
  def parent: Option[Namespace]

  def parent_=(parent: Option[Namespace])

  /**
    * Get components of current namespace
    * @return Seq of [[Component]]
    */
  def components: immutable.Seq[Component]

  def components_=(components: immutable.Seq[Component])

  /**
    * Get languages
    * @return Seq of [[Language]]
    */
  def languages: immutable.Seq[Language]

  def languages_=(languages: immutable.Seq[Language])

  /**
    * Get fully qualified name
    * @return [[Fqn]]
    */
  def fqn: Fqn

  def rootNamespace: Namespace = parent.map(_.rootNamespace).getOrElse(this)

  /**
    * Get all components of current namespace
    * @return Seq of [[Component]]
    */
  def allComponents: Seq[Component] = components ++ subNamespaces.flatMap(_.allComponents)

  /**
    * Get all namespaces
    * @return Seq of [[Namespace]]
    */
  def allNamespaces: Seq[Namespace] = this +: subNamespaces.flatMap(_.allNamespaces)
}