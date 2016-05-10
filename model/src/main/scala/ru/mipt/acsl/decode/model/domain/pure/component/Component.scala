package ru.mipt.acsl.decode.model.domain.pure.component

import ru.mipt.acsl.decode.model.domain.{HasInfo, HasOptionId, NamespaceAware}
import ru.mipt.acsl.decode.model.domain.pure.component.message.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.domain.pure.types.StructType
import ru.mipt.acsl.decode.model.domain.pure.Referenceable

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Component extends HasInfo with Referenceable with HasOptionId with NamespaceAware {
  def statusMessages: immutable.Seq[StatusMessage]
  def eventMessages: immutable.Seq[EventMessage]
  def commands: immutable.Seq[Command]
  def baseType: Option[StructType]
  def subComponents: immutable.Seq[ComponentRef]
}
