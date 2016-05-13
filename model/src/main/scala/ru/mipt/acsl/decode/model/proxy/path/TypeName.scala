package ru.mipt.acsl.decode.model.proxy.path

import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
case class TypeName(typeName: ElementName) extends ProxyElementName {
  override def mangledName: ElementName = typeName
  override def toString: String = typeName.asMangledString
}
