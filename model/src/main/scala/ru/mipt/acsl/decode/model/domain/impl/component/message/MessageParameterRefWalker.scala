package ru.mipt.acsl.decode.model.domain.impl.component.message

import scala.util.{Failure, Success, Try}
import ru.mipt.acsl.decode.model.domain.impl.component._
import ru.mipt.acsl.decode.model.domain.impl.types.{DecodeType, StructField}
import ru.mipt.acsl.decode.model.domain.pure.component.messages._
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

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
  else if (path.isEmpty)
    structField.get.typeUnit.t
  else
    path.foldLeft(structField.get.typeUnit.t)(TypeMessageParameterPathWalker)

  private def findSubComponent(elementName: ElementName): Option[Try[Unit]] =
    component.subComponents.find(elementName.asMangledString == _.aliasOrMangledName).map { subComponent =>
      component = subComponent.component
      structField = None
      Success()
    }

  private def findBaseTypeField(elementName: ElementName): Option[Try[Unit]] =
    component.baseType.flatMap(_.fields.find(elementName == _.name).map { f =>
      structField = Some(f)
      Success()
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
