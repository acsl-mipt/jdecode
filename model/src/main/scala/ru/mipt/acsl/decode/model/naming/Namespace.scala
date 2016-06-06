package ru.mipt.acsl.decode.model.naming

import ru.mipt.acsl.decode.model.{HasInfo, LocalizedString, Referenceable}
import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.registry.Measure
import ru.mipt.acsl.decode.model.types.Alias
import ru.mipt.acsl.decode.model.types.{Const, DecodeType}

import scala.collection.mutable

/**
  * @author Artem Shein
  */

/**
  * Mutable Namespace
  */
trait Namespace extends Referenceable with Container with HasName with HasInfo {

  def alias: Alias.NsNs

  /**
    * Get measures of the current namespace
    *
    * @return Seq of [[Measure]]
    */
  def measures: Seq[Measure] = objects.filter(_.isInstanceOf[Measure]).map(_.asInstanceOf[Measure])

  /**
    * Get types of current namespace
    *
    * @return Seq of [[DecodeType]]
    */
  def types: Seq[DecodeType] = objects.filter(_.isInstanceOf[DecodeType]).map(_.asInstanceOf[DecodeType])

  /**
    * Get subset of current namespaces
    *
    * @return Seq of [[Namespace]]
    */
  def subNamespaces: Seq[Namespace] = objects.flatMap{
    case n: Namespace => Some(n)
    case _ => None
  }

  /**
    * Get parent [[Namespace]] of current namespace
    *
    * @return Parent [[Namespace]] if it defined, otherwise - None
    */
  def parent: Option[Namespace]

  def parent_=(ns: Option[Namespace]): Unit

  def isRoot: Boolean = name == Namespace.RootName

  override def name: ElementName = alias.name

  override def info: LocalizedString = alias.info

  /**
    * Get components of current namespace
    *
    * @return Seq of [[Component]]
    */
  def components: Seq[Component] = objects.flatMap {
    case c: Component => Seq(c)
    case _ => Seq.empty
  }

  def alias(name: ElementName): Option[Alias] = objects.flatMap {
    case a: Alias if a.name == name => Seq(a)
    case _ => Seq.empty
  } match {
    case s if s.size == 1 => Some(s.head)
    case _ => None
  }

  def aliases: Seq[Alias] = objects.flatMap {
    case a: Alias => Seq(a)
    case _ => Seq.empty
  }

  def consts: Seq[Const] = objects.filter(_.isInstanceOf[Const]).map(_.asInstanceOf[Const])

  def rootNamespace: Namespace = parent.map(_.rootNamespace).getOrElse(this)

  /**
    * Get all components of current namespace
    *
    * @return Seq of [[Component]]
    */
  def allComponents: Seq[Component] = components ++ subNamespaces.flatMap(_.allComponents)

  /**
    * Get all namespaces
    *
    * @return Seq of [[Namespace]]
    */
  def allNamespaces: Seq[Namespace] = this +: subNamespaces.flatMap(_.allNamespaces)

  /**
    * Get fully qualified name
    *
    * @return [[Fqn]]
    */
  def fqn: Fqn = {
    val parts = mutable.Buffer[ElementName]()
    var currentNamespace = this
    while (currentNamespace.parent.isDefined) {
      if (!currentNamespace.isRoot) {
        parts += currentNamespace.name
      }
      currentNamespace = currentNamespace.parent.get
    }
    if (!currentNamespace.isRoot) {
      parts += currentNamespace.name
    }
    Fqn(parts.reverse)
  }

}

object Namespace {

  val RootName = ElementName.newFromMangledName("%root")

  private class NamespaceImpl(var alias: Alias.NsNs, var parent: Option[Namespace],
                     var objects: Seq[Referenceable])
    extends Namespace

  def apply(alias: Alias.NsNs, parent: Option[Namespace] = None, objects: Seq[Referenceable] = Seq.empty): Namespace =
    new NamespaceImpl(alias, parent, objects)

  def newRoot: Namespace = {
    val alias = Alias.NsNs(RootName, LocalizedString.empty)(null, null)
    alias.obj = Namespace(alias, None)
    alias.parent = alias.obj
    alias.obj
  }

  // TODO: refactoring -- use fold instead
  def apply(fqn: Fqn, rootNamespace: Namespace, info: LocalizedString): Namespace = {
    var currentNamespace = rootNamespace
    val size = fqn.size
    for ((part, i) <- fqn.parts.zipWithIndex) {
      val alias = Alias.NsNs(part, if (i == size - 1) info else LocalizedString.empty)(currentNamespace, null)
      val ns = Namespace(alias, Some(currentNamespace))
      alias.obj = ns
      currentNamespace.objects ++= Seq(ns, alias)
      currentNamespace = ns
    }
    currentNamespace
  }

}
