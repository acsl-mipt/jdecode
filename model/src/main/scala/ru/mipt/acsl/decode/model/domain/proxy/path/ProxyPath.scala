package ru.mipt.acsl.decode.model.domain.proxy.path

import ru.mipt.acsl.decode.model.domain.impl.naming.ElementName
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn}

/**
  * @author Artem Shein
  */
class ProxyPath(val ns: Fqn, val element: ProxyElementName) {
  def mangledName: ElementName = ElementName.newFromMangledName(s"${ns.asMangledString}.${element.mangledName.asMangledString}")

  override def toString: String = s"ProxyPath{${ns.asMangledString}.$element}"
}

object ProxyPath {

  def apply(fqn: Fqn): ProxyPath = apply(fqn.copyDropLast, fqn.last)

  def apply(nsFqn: Fqn, name: ElementName): ProxyPath = new ProxyPath(nsFqn, TypeName(name))

  def apply(nsFqn: Fqn, element: ProxyElementName): ProxyPath = new ProxyPath(nsFqn, element)

}
