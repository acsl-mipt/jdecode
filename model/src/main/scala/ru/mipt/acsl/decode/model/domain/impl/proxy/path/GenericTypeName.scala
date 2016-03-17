package ru.mipt.acsl.decode.model.domain.impl.proxy.path

import ru.mipt.acsl.decode.model.domain.impl.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

import scala.collection.immutable

/**
  * @author Artem Shein
  */
case class GenericTypeName(typeName: ElementName, genericArgumentPaths: immutable.Seq[Option[ProxyPath]])
  extends ProxyElementName {
  override def mangledName: ElementName = ElementName.newFromMangledName(typeName.asMangledString +
    genericArgumentPaths.map(_.map(_.mangledName)
      .getOrElse(ElementName.newFromMangledName("void"))).mkString("<", ",", ">"))
}
