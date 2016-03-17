package ru.mipt.acsl.decode.model.domain.impl.proxy.path

import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
case class TypeName(typeName: ElementName) extends ProxyElementName {
  override def mangledName: ElementName = typeName
}
