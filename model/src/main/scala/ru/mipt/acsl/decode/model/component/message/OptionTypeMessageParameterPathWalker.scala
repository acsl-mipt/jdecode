package ru.mipt.acsl.decode.model
package component
package message

import ru.mipt.acsl.decode.model.types.{AliasType, DecodeType, GenericTypeSpecialized, NativeType, StructType, SubType}

/**
  * @author Artem Shein
  */
case object OptionTypeMessageParameterPathWalker
  extends ((DecodeType, MessageParameterPathElement) => Option[DecodeType]) {

  override def apply(t: DecodeType, pathElement: MessageParameterPathElement): Option[DecodeType] = t match {
    case t: SubType => apply(t.baseType, pathElement)
    case a: GenericTypeSpecialized if a.genericType.isArray =>
      if (!pathElement.isRight)
        sys.error("invalid token")
      Some(a.genericTypeArgumentsProxy.head.obj)
    case t: StructType =>
      if (pathElement.isRight)
        sys.error(s"invalid token ${pathElement.right.get}")
      val name = pathElement.left.get
      Some(t.fields.find(_.name == name)
        .getOrElse {
          sys.error(s"Field '$name' not found in struct '$t'")
        }.typeUnit.t)
    case t: AliasType => apply(t.baseType, pathElement)
    case _ => None
  }

}
