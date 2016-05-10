package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn}

import scala.collection.immutable

package object naming {

  implicit class FqnHelper(val fqn: Fqn) {
    def newRootDecodeNamespaceForFqn: Namespace = {
      fqn.parts.reverseIterator.drop(1).foldLeft(Namespace(fqn.last)) { (ns, name) =>
        val parentNamespace = Namespace(name)
        ns.parent = Some(parentNamespace)
        parentNamespace.subNamespaces = parentNamespace.subNamespaces :+ ns
        parentNamespace
      }
    }

    // TODO: refactoring -- use fold instead
    def newNamespaceForFqn(info: LocalizedString = LocalizedString.empty): Namespace = {
      var currentNamespace: Option[Namespace] = None
      val size = fqn.size
      for ((part, i) <- fqn.parts.zipWithIndex) {
        val ns = Namespace(part, parent = currentNamespace, info = if (i == size - 1) info else LocalizedString.empty)
        currentNamespace = Some(ns)
        for (parent <- ns.parent) {
          parent.subNamespaces :+= ns
        }
      }
      currentNamespace.getOrElse(sys.error("assertion error"))
    }

  }

  implicit class PureNamespaceHelper(val ns: Namespace) {
    /**
      * String representation of current namespace
      *
      * @return String namespace
      */
    def asString: String = ns.name.asMangledString

    /**
      * Get fully qualified name
      *
      * @return [[Fqn]]
      */
    def fqn: Fqn = {
      val parts: scala.collection.mutable.Buffer[ElementName] = scala.collection.mutable.Buffer[ElementName]()
      var currentNamespace = ns
      while (currentNamespace.parent.isDefined) {
        parts += currentNamespace.name
        currentNamespace = currentNamespace.parent.get
      }
      parts += currentNamespace.name
      Fqn(parts.reverse)
    }
  }

  implicit class NamespacesHelper(val nses: immutable.Seq[Namespace]) {

    def mergeWith(namespace: Namespace): immutable.Seq[Namespace] = {
      val nsName = namespace.name
      nses.find(_.name == nsName)
        .map{ns => ns.mergeNamespace(namespace); nses}
        .getOrElse(nses :+ namespace)
    }

    def mergeRoot: immutable.Seq[Namespace] = {
      var result = immutable.Seq.empty[Namespace]
      nses.foreach{ ns => result = result.mergeWith(ns) }
      result
    }
  }

  implicit class NamespaceHelper(val ns: Namespace) {

    def rootNamespace: Namespace = ns.parent.map(_.rootNamespace).getOrElse(ns)

    /**
      * Get all components of current namespace
      *
      * @return Seq of [[Component]]
      */
    def allComponents: Seq[Component] = ns.components ++ ns.subNamespaces.flatMap(_.allComponents)

    /**
      * Get all namespaces
      *
      * @return Seq of [[Namespace]]
      */
    def allNamespaces: Seq[Namespace] = ns +: ns.subNamespaces.flatMap(_.allNamespaces)

    def mergeNamespace(namespace: Namespace) = {
      val subNamespaces = ns.subNamespaces
      namespace.subNamespaces.foreach { subNs =>
        ns.subNamespaces = subNamespaces.mergeWith(subNs)
        subNs.parent = Some(ns)
      }

      val units = ns.units
      namespace.units.foreach { u =>
        val name = u.name
        if (units.exists(_.name == name))
          sys.error(s"unit name collision '$name'")
        u.namespace = ns
      }
      ns.units ++= namespace.units

      val types = ns.types
      namespace.types.foreach { t =>
        val name = t.name
        if (types.exists(t2 => t2.name.equals(name)))
          sys.error(s"type name collision '$name'")
        t.namespace = ns
      }
      ns.types ++= namespace.types

      val components = ns.components
      namespace.components.foreach { c =>
        val name = c.name
        if (components.exists(c2 => c2.name == name))
          sys.error(s"component name collision '$name'")
        c.namespace = ns
      }
      ns.components ++= namespace.components
    }
  }

}