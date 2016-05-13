package ru.mipt.acsl.decode.model
package component
package message

import scala.util.{Failure, Success, Try}
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.types.{DecodeType, StructField}

/**
  * @author Artem Shein
  */
class MessageParameterRefWalker(var component: Component, var structField: Option[StructField] = None,
                                var path: MessageParameterPath = MessageParameterPath.empty)
  extends MessageParameterRef {

  while (structField.isEmpty)
    walkOne()

  override def t: DecodeType = if (structField.isEmpty)
    component.baseType.get
  else
    TypeMessageParameterPathWalker(structField.get.typeUnit.t, path.head)

  override def resultType: DecodeType = if (structField.isEmpty)
    component.baseType.get
  else
    path.foldLeft(structField.get.typeUnit.t)(TypeMessageParameterPathWalker)

  private def findSubComponent(elementName: ElementName): Option[Try[Unit]] =
    component.subComponents.find(elementName.asMangledString == _.aliasOrMangledName).map { subComponent =>
      component = subComponent.component
      structField = None
      Success(Unit)
    }

  private def findBaseTypeField(elementName: ElementName): Option[Try[Unit]] =
    component.baseType.flatMap(_.fields.find(elementName == _.name).map { f =>
      structField = Some(f)
      Success(Unit)
    })

  private def findTokenString(token: MessageParameterPathElement): Option[Try[Unit]] =
    token.left.toOption.flatMap { elementName =>
      findSubComponent(elementName).orElse(findBaseTypeField(elementName))
    }

  private def walkOne(): Try[Unit] = {
    require(path.nonEmpty)
    val token = path.head
    val fail = () => Try[Unit] { Failure(new IllegalStateException(s"can't walk $token for $this")) }
    path = path.tail
    structField match {
      case Some(_) => fail()
      case _ => findTokenString(token).getOrElse { fail() }
    }
  }
}
