package ru.mipt.acsl.decode.model.domain.impl.proxy.path

import ru.mipt.acsl.decode.model.domain.impl.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.ArraySize

/**
  * @author Artem Shein
  */
case class ArrayTypePath(baseTypePath: ProxyPath, arraySize: ArraySize) extends ProxyElementName {
  def mangledName: ElementName = ElementName.newFromMangledName(s"[${baseTypePath.mangledName.asMangledString}]")
}
