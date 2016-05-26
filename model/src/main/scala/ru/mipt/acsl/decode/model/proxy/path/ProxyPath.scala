package ru.mipt.acsl.decode.model.proxy.path

import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn}

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

  def apply(fqn: Fqn): ProxyPath = apply(fqn.copyDropLast, fqn.last)

  def apply(nsFqn: Fqn, name: ElementName): ProxyPath = FqnElement(nsFqn, TypeName(name))

  def apply(nsFqn: Fqn, element: ProxyElementName): ProxyPath = FqnElement(nsFqn, element)

  def fromLiteral(value: String): ProxyPath = Literal(value)

}
