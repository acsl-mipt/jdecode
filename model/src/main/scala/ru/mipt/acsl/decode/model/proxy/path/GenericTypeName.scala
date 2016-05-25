package ru.mipt.acsl.decode.model.proxy.path

import ru.mipt.acsl.decode.model.naming.ElementName

import scala.collection.immutable

/**
  * @author Artem Shein
  */
case class GenericTypeName(typeName: ElementName, genericArgumentPaths: immutable.Seq[ProxyPath])
  extends ProxyElementName {
  override def mangledName: ElementName = ElementName.newFromMangledName(typeName.asMangledString +
    genericArgumentPaths.map(_.mangledName.asMangledString).mkString("<", ",", ">"))
}