package ru.mipt.acsl.decode.model.proxy.path

import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
case class GenericTypeName(typeName: ElementName, genericArgumentPaths: Seq[ProxyPath])
  extends ProxyElementName {
  override def mangledName: ElementName = ElementName.newInstanceFromMangledName(typeName.mangledNameString +
    genericArgumentPaths.map(_.mangledName.mangledNameString).mkString("[", ",", "]"))
}
