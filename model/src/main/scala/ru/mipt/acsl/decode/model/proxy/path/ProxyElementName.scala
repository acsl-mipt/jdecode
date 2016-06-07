package ru.mipt.acsl.decode.model.proxy.path

import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
abstract class ProxyElementName {

  def mangledName: ElementName

  override def toString: String = mangledName.mangledNameString

}
