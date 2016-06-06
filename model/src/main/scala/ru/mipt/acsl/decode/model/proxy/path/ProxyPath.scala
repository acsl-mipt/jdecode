package ru.mipt.acsl.decode.model.proxy.path

import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn, Namespace}

/**
  * @author Artem Shein
  */
sealed trait ProxyPath {

  def mangledName: ElementName

}

object ProxyPath {

  case class FqnElement(ns: Fqn, element: ProxyElementName) extends ProxyPath {

    override def mangledName: ElementName =
      ElementName.newFromMangledName(s"${ns.asMangledString}.${element.mangledName.asMangledString}")

    override def toString: String = s"ProxyPath.Element{${ns.asMangledString}.$element}"

  }

  case class Literal(value: String) extends ProxyPath {

    override def mangledName: ElementName =
      ElementName.newFromMangledName(s"$value")

    override def toString: String = s"ProxyPath.Literal{$value}"

  }

  def apply(elementName: ProxyElementName): ProxyPath =
    apply(Fqn.DecodeNamespace, elementName)

  def apply(fqn: Fqn): ProxyPath = apply(fqn.copyDropLast, fqn.last)

  def apply(nsFqn: Fqn, name: ElementName): ProxyPath = FqnElement(nsFqn, TypeName(name))

  def apply(nsFqn: Fqn, element: ProxyElementName): ProxyPath = FqnElement(nsFqn, element)

  def apply(elementFqn: Fqn, defaultNamespace: Namespace): ProxyPath =
    if (elementFqn.size > 1)
      apply(elementFqn.copyDropLast, TypeName(elementFqn.last))
    else
      apply(defaultNamespace.fqn, TypeName(elementFqn.last))

  def fromLiteral(value: String): ProxyPath = Literal(value)

}
