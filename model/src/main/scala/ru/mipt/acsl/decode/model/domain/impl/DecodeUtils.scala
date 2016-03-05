package ru.mipt.acsl.decode.model.domain.impl

import java.net.{URI, URLDecoder, URLEncoder}

import com.google.common.base.Charsets
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.types.{Fqn, Namespace, NamespaceImpl}

import scala.collection.immutable

/**
  * Created by metadeus on 12.02.16.
  */
object DecodeUtils {
  // TODO: refactoring -- use fold instead
  def getOrCreateNamespaceByFqn(registry: Registry, namespaceFqn: String): Namespace = {
    require(!namespaceFqn.isEmpty)

    val namespaces = registry.rootNamespaces
    var namespace: Option[Namespace] = None

    "\\.".r.split(namespaceFqn).foreach { nsName =>
      val parentNamespace = namespace
      namespace = Some(namespaces.find(_.name.asMangledString == nsName).getOrElse {
        val newNamespace = Namespace(ElementName.newFromMangledName(nsName), parent = parentNamespace)
        parentNamespace match {
          case Some(parentNs) => parentNs.subNamespaces = parentNs.subNamespaces :+ newNamespace
          case _ => registry.rootNamespaces = registry.rootNamespaces :+ newNamespace
        }
        newNamespace
      })
    }
    namespace.getOrElse(sys.error("assertion error"))
  }

  // TODO: refactoring
  def getNamespaceByFqn(registry: Registry, namespaceFqn: Fqn): Option[Namespace] = {
    var namespaces = registry.rootNamespaces
    var namespace: Option[Namespace] = None
    for (nsName <- namespaceFqn.parts) {
      namespace = namespaces.find(_.name == nsName)
      if (namespace.isEmpty)
      {
        return namespace
      }
      else
      {
        namespaces = namespace.get.subNamespaces
      }
    }
    namespace
  }

  def getUriForNamespaceAndName(namespaceFqn: Fqn, name: ElementName): URI = {
    val namespaceNameParts = namespaceFqn.parts :+ name
    URI.create("/" + namespaceNameParts.map(_.asMangledString).map(s => URLEncoder.encode(s, Charsets.UTF_8.name()))
      .mkString("/"))
  }

  def getUriParts(uri: URI): Seq[String] = {
    val s = uri.toString
    "\\/".r.split(s.substring(1)).map(part => URLDecoder.decode(part, Charsets.UTF_8.name())).toSeq
  }

  def newRootDecodeNamespaceForFqn(namespaceFqn: Fqn): Namespace = {
    namespaceFqn.parts.reverseIterator.drop(1).foldLeft(Namespace(namespaceFqn.last)) { (ns, name) =>
      val parentNamespace = Namespace(name)
      ns.parent = Some(parentNamespace)
      parentNamespace.subNamespaces = parentNamespace.subNamespaces :+ ns
      parentNamespace
    }
  }

  // TODO: refactoring -- use fold instead
  def newNamespaceForFqn(fqn: Fqn, info: Option[String] = None): Namespace = {
    var currentNamespace: Option[Namespace] = None
    val size = fqn.size
    for ((part, i) <- fqn.parts.zipWithIndex) {
      val ns = Namespace(part, parent = currentNamespace, info = if (i == size - 1) info else None)
      currentNamespace = Some(ns)
      for (parent <- ns.parent) {
        parent.subNamespaces = parent.subNamespaces :+ ns
      }
    }
    currentNamespace.getOrElse(sys.error("assertion error"))
  }

  def getNamespaceFqnFromUri(uri: URI): Fqn = {
    val uriParts = getUriParts(uri)
    Fqn(uriParts.take(uriParts.size - 1).map(ElementName.newFromMangledName))
  }

  def getUriForSourceTypeFqnString(typeFqnString: String, defaultNamespaceFqn: Fqn): URI = {
    require(!typeFqnString.contains("/"), s"illegal type fqn '$typeFqnString'")
    val genericStartIndex = processQuestionMarks(normalizeSourceTypeString(typeFqnString)).indexOf('<')
    if (genericStartIndex != -1)
    {
      val namespaceFqn = Fqn.newFromSource(typeFqnString.substring(0, genericStartIndex))
      return getUriForTypeNamespaceNameGenericArguments(
        if (namespaceFqn.size > 1) namespaceFqn.copyDropLast else defaultNamespaceFqn,
        namespaceFqn.last, typeFqnString.substring(genericStartIndex))
    }
    val namespaceFqn = Fqn.newFromSource(typeFqnString)
    getUriForNamespaceAndName(namespaceFqn.copyDropLast, namespaceFqn.last)
  }

  def  getUriForTypeNamespaceNameGenericArguments(namespaceFqn: Fqn, typeName: ElementName,
                                                  typeGenericArguments: String): URI = {
    URI.create("/" + URLEncoder.encode((namespaceFqn.parts :+ typeName).map(_.asMangledString)
      .map(s => URLEncoder.encode(s, Charsets.UTF_8.name())).mkString("/") + typeGenericArguments,
      Charsets.UTF_8.name()))
  }

  def processQuestionMarks(typeString: String): String = {
    if (typeString.endsWith("?"))
    {
      DecodeConstants.SYSTEM_NAMESPACE_FQN.asMangledString + ".optional<" +
        "\\,".r.split(typeString.substring(0, typeString.length() - 1))
          .map(DecodeUtils.processQuestionMarks).mkString(",") + ">"
    } else {
      typeString
    }
  }

  def normalizeSourceTypeString(typeString: String): String = " ".r.replaceAllIn(typeString, "")

  def typeUriToTypeName(uri: String): String = typeFqnStringFromUriString(uri.toString)

  // TODO: remove me
  //def uriToOptionalMaybeProxyType(typeUriString: String): Option[MaybeProxy[DecodeType]] =
  //  if (typeUriString.isEmpty) None else Some(MaybeProxy.proxy(ProxyPath(typeUriString)))

  def typeFqnStringFromUriString(typeUriString: String): String =
    URLDecoder.decode(typeUriString, Charsets.UTF_8.name()).substring(1).replaceAllLiterally("/", ".")

  def mergeNamespaceTo(targetNamespace: Namespace, namespace: Namespace): Namespace = {
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
      val name = t.name
      if (types.exists(t2 => t2.name.equals(name)))
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

  private def mergeNamespaceToNamespacesList(list: immutable.Seq[Namespace],
                                             namespace: Namespace): immutable.Seq[Namespace] = {
    val nsName = namespace.name
    list.find(_.name == nsName)
      .map{ns => mergeNamespaceTo(ns, namespace); list}
      .getOrElse(list :+ namespace)
  }

  def mergeRootNamespaces(namespaces: Traversable[Namespace]): immutable.Seq[Namespace] = {
    var result = immutable.Seq.empty[Namespace]
    namespaces.foreach{ ns => result = mergeNamespaceToNamespacesList(result, ns) }
    result
  }
}
