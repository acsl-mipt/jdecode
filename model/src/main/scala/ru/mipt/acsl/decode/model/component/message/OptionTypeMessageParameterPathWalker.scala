package ru.mipt.acsl.decode.model
package component
package message

import ru.mipt.acsl.decode.model.types.{DecodeType, GenericTypeSpecialized, StructType, SubType}

/**
  * @author Artem Shein
  */
case object OptionTypeMessageParameterPathWalker
  extends ((DecodeType, MessageParameterPathElement) => Option[DecodeType]) {

  override def apply(t: DecodeType, pathElement: MessageParameterPathElement): Option[DecodeType] = t match {
    case t: SubType => apply(t.baseType, pathElement)
    case a: GenericTypeSpecialized if a.genericType.isArray =>
      if (!pathElement.isArrayRange)
        sys.error("invalid token")
      Some(a.genericTypeArgumentsProxy.head.obj)
    case t: StructType =>
      if (pathElement.isArrayRange)
        sys.error(s"invalid token ${pathElement.arrayRange().get()}")
      val name = pathElement.elementName().get()
      Some(t.field(name).getOrElse {
          sys.error(s"Field '$name' not found in struct '$t'")
        }.typeMeasure.t)
    case _ => None
  }

}
