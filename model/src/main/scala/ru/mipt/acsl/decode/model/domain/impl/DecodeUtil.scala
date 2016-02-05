package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.DecodeNamespace

import scala.collection.immutable

/**
  * @author Artem Shein
  */
object DecodeUtil {

  def mergeNamespaceTo(targetNamespace: DecodeNamespace, namespace: DecodeNamespace): DecodeNamespace = {
    val subNamespaces = targetNamespace.subNamespaces
    namespace.subNamespaces.foreach { ns =>
      targetNamespace.subNamespaces = mergeNamespaceToNamespacesList(subNamespaces, ns)
      ns.parent = Some(targetNamespace)
    }

    val units = targetNamespace.units
    namespace.units.foreach { u =>
        val name = u.name
        if (units.exists(_.name == name))
          sys.error(s"unit name collision '$name'")
        u.namespace = targetNamespace
    }
    targetNamespace.units ++= namespace.units

    val types = targetNamespace.types
    namespace.types.foreach { t =>
        val name = t.optionName
        if (types.exists(t2 => t2.optionName == name))
          sys.error(s"type name collision '$name'")
        t.namespace = targetNamespace
    }
    targetNamespace.types ++= namespace.types

    val components = targetNamespace.components
    namespace.components.foreach { c =>
        val name = c.name
        if (components.exists(c2 => c2.name == name))
          sys.error(s"component name collision '$name'")
        c.namespace = targetNamespace
    }
    targetNamespace.components ++= namespace.components

    targetNamespace
  }

  private def mergeNamespaceToNamespacesList(list: immutable.Seq[DecodeNamespace],
                                             namespace: DecodeNamespace): immutable.Seq[DecodeNamespace] = {
    val nsName = namespace.name
    list.find(_.name == nsName)
      .map{ns => mergeNamespaceTo(ns, namespace); list}
      .getOrElse(list :+ namespace)
  }

  def mergeRootNamespaces(namespaces: Traversable[DecodeNamespace]): immutable.Seq[DecodeNamespace] = {
    var result = immutable.Seq.empty[DecodeNamespace]
    namespaces.foreach{ ns => result = mergeNamespaceToNamespacesList(result, ns) }
    result
  }
}
