package ru.mipt.acsl.decode.model.domain.proxy.path

import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
abstract class ProxyElementName {
  def mangledName: ElementName

  override def toString: String = mangledName.asMangledString
}
