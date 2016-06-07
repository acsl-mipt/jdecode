package ru.mipt.acsl.decode.model.component.message

import java.util.Optional

import org.jetbrains.annotations.Nullable
import ru.mipt.acsl.decode.model.component.{Component, MessageParameterPath, MessageParameterPathElement}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.types.{DecodeType, StructField}

/**
  * @author Artem Shein
  */
class MessageParameterRefWalker(var component: Component, @Nullable var _structField: StructField = null,
                                var path: MessageParameterPath = MessageParameterPath.newInstance())
  extends MessageParameterRef {

  while (_structField == null)
    walkOne()

  override def t: DecodeType = if (_structField == null)
    component.baseType.get
  else
    TypeMessageParameterPathWalker(_structField.typeMeasure.t, path.head)

  override def resultType: DecodeType = if (_structField == null)
    component.baseType.get
  else
    path.elements().foldLeft(_structField.typeMeasure.t)(TypeMessageParameterPathWalker)

  private def findSubComponent(elementName: ElementName): Option[Try[Unit]] =
    component.subComponents.find(elementName == _.name).map { subComponent =>
      component = subComponent.obj.obj
      _structField = null
      Success(Unit)
    }

  private def findBaseTypeField(elementName: ElementName): Option[Try[Unit]] =
    component.baseType.flatMap(_.field(elementName).map { f =>
      _structField = f
      Success(Unit)
    })

  private def findTokenString(token: MessageParameterPathElement): Option[Try[Unit]] =
    if (token.elementName().isPresent) {
      val elementName = token.elementName().get()
      findSubComponent(elementName).orElse(findBaseTypeField(elementName))
    } else {
      None
    }

  private def walkOne(): Try[Unit] = {
    require(!path.elements().isEmpty)
    val token = path.head
    val fail = () => Try[Unit] { Failure(new IllegalStateException(s"can't walk $token for $this")) }
    path = path.tail
    if (structField != null)
      fail()
    else
      findTokenString(token).getOrElse { fail() }
  }

  override def structField(): Optional[StructField] = Optional.ofNullable(_structField)
}
