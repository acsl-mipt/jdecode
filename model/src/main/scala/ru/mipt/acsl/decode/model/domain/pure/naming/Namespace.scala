package ru.mipt.acsl.decode.model.domain.pure.naming

import ru.mipt.acsl.decode.model.domain.pure.component.Component
import ru.mipt.acsl.decode.model.domain.pure.{HasInfo, HasNameAndInfo, Referenceable}
import ru.mipt.acsl.decode.model.domain.pure.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.pure.types.DecodeType

import scala.collection.immutable

/**
  * @author Artem Shein
  */

/**
  * Namespace trait
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
}
