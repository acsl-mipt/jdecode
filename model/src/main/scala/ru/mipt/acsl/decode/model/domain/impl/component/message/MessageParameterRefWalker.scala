package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.pure.MessageParameterToken

import scala.util.{Failure, Success, Try}
import ru.mipt.acsl.decode.model.domain.impl.component._
import ru.mipt.acsl.decode.model.domain.impl.types.{DecodeType, StructField}

/**
  * @author Artem Shein
  */
class MessageParameterRefWalker(var component: Component, var structField: Option[StructField] = None,
                                var subTokens: Seq[MessageParameterToken] = Seq.empty)
  extends MessageParameterRef {

  while (structField.isEmpty)
    walkOne()

  override def t: DecodeType = if (structField.isEmpty)
    component.baseType.get
  else if (subTokens.isEmpty)
    structField.get.typeUnit.t
  else
    subTokens.foldLeft(structField.get.typeUnit.t)(TokenTypeWalker)

  private def findSubComponent(tokenString: String): Option[Try[Unit]] =
    component.subComponents.find(tokenString == _.aliasOrMangledName).map { subComponent =>
      component = subComponent.component
      structField = None
      Success()
    }

  private def findBaseTypeField(tokenString: String): Option[Try[Unit]] =
    component.baseType.flatMap(_.fields.find(tokenString == _.name.asMangledString).map { f =>
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
