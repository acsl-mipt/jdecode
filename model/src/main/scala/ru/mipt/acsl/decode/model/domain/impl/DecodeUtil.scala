package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain.DecodeNamespace

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * @author Artem Shein
  */
object DecodeUtil {

  def mergeNamespaceTo(targetNamespace: DecodeNamespace, namespace: DecodeNamespace) {
    val subNamespaces = targetNamespace.subNamespaces
    namespace.subNamespaces.foreach(ns => { mergeNamespaceToNamespacesList(subNamespaces, ns); ns.parent = Option.apply(targetNamespace) })

    val units = targetNamespace.units
    namespace.units.foreach(u => {
        val name = u.name
        if (units.exists(_.name == name))
          sys.error(s"unit name collision '$name'")
        u.namespace = targetNamespace
    })
    units ++= namespace.units

    val types = targetNamespace.types
    namespace.types.foreach(t => {
        val name = t.optionalName
        if (types.exists(t2 => t2.optionalName == name))
          sys.error(s"type name collision '$name'")
        t.namespace = targetNamespace
    })
    types ++= namespace.types

    val components = targetNamespace.components
    namespace.components.foreach(c => {
        val name = c.name
        if (components.exists(c2 => c2.name == name))
          sys.error(s"component name collision '$name'")
        c.namespace = targetNamespace
    })
    components ++= namespace.components
  }

  private def mergeNamespaceToNamespacesList(list: mutable.Buffer[DecodeNamespace], namespace: DecodeNamespace) {
    val nsName = namespace.name
    val targetNamespace = list.find(_.name == nsName)

    if (targetNamespace.isDefined)
      mergeNamespaceTo(targetNamespace.get, namespace)
    else
      list += namespace
  }

  def mergeRootNamespaces(namespaces: mutable.Buffer[DecodeNamespace]): mutable.Buffer[DecodeNamespace] = {
    val result = new ArrayBuffer[DecodeNamespace]()
    namespaces.foreach(mergeNamespaceToNamespacesList(result, _))
    result
  }
}
