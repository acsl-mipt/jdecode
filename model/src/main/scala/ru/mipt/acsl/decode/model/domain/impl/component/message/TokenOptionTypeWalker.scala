package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.aliases.MessageParameterToken
import ru.mipt.acsl.decode.model.domain.types._

/**
  * @author Artem Shein
  */
case object TokenOptionTypeWalker extends ((DecodeType, MessageParameterToken) => Option[DecodeType]) {
  override def apply(t: DecodeType, token: MessageParameterToken): Option[DecodeType] = t match {
    case t: SubType => apply(t.baseType.obj, token)
    case t: ArrayType =>
      if (!token.isRight)
        sys.error("invalid token")
      Some(t.baseType.obj)
    case t: StructType =>
      if (token.isRight)
        sys.error(s"invalid token ${token.right.get}")
      val name = token.left.get
      Some(t.fields.find(_.name.asMangledString == name)
        .getOrElse {
          sys.error(s"Field '$name' not found in struct '$t'")
        }.typeUnit.t.obj)
    case t: AliasType => apply(t.baseType.obj, token)
    case _ => None
  }
}
