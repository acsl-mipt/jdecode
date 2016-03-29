package ru.mipt.acsl.decode.model.domain.impl.proxy.path

import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
abstract class ProxyElementName {
  def mangledName: ElementName

  override def toString: String = mangledName.asMangledString
}
