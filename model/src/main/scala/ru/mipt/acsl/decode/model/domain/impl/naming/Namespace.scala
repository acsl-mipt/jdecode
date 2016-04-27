package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.pure.{naming => n}
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.registry.DecodeUnit
import ru.mipt.acsl.decode.model.domain.impl.types.DecodeType

import scala.collection.immutable

/**
  * @author Artem Shein
  */

/**
  * Mutable Namespace
  */
trait Namespace extends pure.naming.Namespace {
  override def parent: Option[Namespace]
  def parent_=(ns: Option[Namespace]): Unit
  override def subNamespaces: immutable.Seq[Namespace]
  def subNamespaces_=(nses: immutable.Seq[Namespace]): Unit
  override def units: immutable.Seq[DecodeUnit]
  def units_=(u: immutable.Seq[DecodeUnit]): Unit
  override def types: immutable.Seq[DecodeType]
  def types_=(t: immutable.Seq[DecodeType]): Unit
  override def components: immutable.Seq[Component]
  def components_=(c: immutable.Seq[Component]): Unit
}

object Namespace {
  def apply(name: n.ElementName, info: LocalizedString = LocalizedString.empty, parent: Option[Namespace] = None,
            types: immutable.Seq[DecodeType] = immutable.Seq.empty,
            units: immutable.Seq[DecodeUnit] = immutable.Seq.empty,
            subNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty,
            components: immutable.Seq[Component] = immutable.Seq.empty): Namespace =
    new NamespaceImpl(name, info, parent, types, units, subNamespaces, components)
}
