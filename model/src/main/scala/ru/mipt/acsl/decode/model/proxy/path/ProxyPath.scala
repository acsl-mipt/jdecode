package ru.mipt.acsl.decode.model.proxy.path

import ru.mipt.acsl.decode.model.naming.ElementName.newInstanceFromMangledName
import ru.mipt.acsl.decode.model.naming.Fqn.DECODE_NAMESPACE
import ru.mipt.acsl.decode.model.naming.{ElementName, _}

/**
  * @author Artem Shein
  */
sealed trait ProxyPath {

  def mangledName: ElementName

}

object ProxyPath {

  case class FqnElement(ns: Fqn, element: ProxyElementName) extends ProxyPath {

    override def mangledName: ElementName =
      ElementName.newInstanceFromMangledName(s"${ns.mangledNameString()}.${element.mangledName.mangledNameString}")

    override def toString: String = s"ProxyPath.Element{${ns.mangledNameString()}.$element}"

  }

  case class Literal(value: String) extends ProxyPath {

    override def mangledName: ElementName =
      ElementName.newInstanceFromMangledName(s"$value")

    override def toString: String = s"ProxyPath.Literal{$value}"

  }

  def apply(elementName: ProxyElementName): ProxyPath =
    apply(Fqn.DECODE_NAMESPACE, elementName)

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
