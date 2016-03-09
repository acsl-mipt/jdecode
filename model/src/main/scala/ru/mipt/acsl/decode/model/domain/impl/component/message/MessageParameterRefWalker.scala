package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.TokenTypeWalker

/**
  * @author Artem Shein
  */
class MessageParameterRefWalker(var component: Component, var structField: Option[StructField] = None,
                                var subTokens: Seq[MessageParameterToken] = Seq.empty)
  extends MessageParameterRef {

  while (structField.isEmpty)
    walkOne()

  override def t: DecodeType = if (structField.isEmpty)
    component.baseType.get.obj
  else if (subTokens.isEmpty)
    structField.get.typeUnit.t.obj
  else
    subTokens.foldLeft(structField.get.typeUnit.t.obj)(TokenTypeWalker)

  private def findSubComponent(tokenString: String): Option[Try[Unit]] =
    component.subComponents.find(tokenString == _.aliasOrMangledName).map { subComponent =>
      component = subComponent.component.obj
      structField = None
      Success()
    }

  private def findBaseTypeField(tokenString: String): Option[Try[Unit]] =
    component.baseType.flatMap(_.obj.fields.find(tokenString == _.name.asMangledString).map { f =>
      structField = Some(f)
      Success()
    })

  private def findTokenString(token: MessageParameterToken): Option[Try[Unit]] =
    token.left.toOption.flatMap { tokenString =>
      findSubComponent(tokenString).orElse(findBaseTypeField(tokenString))
    }

  private def walkOne(): Try[Unit] = {
    require(subTokens.nonEmpty)
    val token = subTokens.head
    val fail = () => Try[Unit] { Failure(new IllegalStateException(s"can't walk $token for $this")) }
    subTokens = subTokens.tail
    structField match {
      case Some(_) => fail()
      case _ => findTokenString(token).getOrElse { fail() }
    }
  }
}
