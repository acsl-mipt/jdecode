package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.component.messages.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.domain.naming.Fqned
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.StructType

import scala.collection.immutable

/**
  * The basic block of the system interface board.
  */
trait Component extends HasInfo with Fqned with Referenceable with HasOptionId with Resolvable with Validatable {
  def statusMessages: immutable.Seq[StatusMessage]

  def statusMessages_=(messages: immutable.Seq[StatusMessage]): Unit

  def eventMessages: immutable.Seq[EventMessage]

  def eventMessages_=(messages: immutable.Seq[EventMessage]): Unit

  def commands: immutable.Seq[Command]

  def commands_=(commands: immutable.Seq[Command]): Unit

  def baseType: Option[MaybeProxy[StructType]]

  def baseType_=(maybeProxy: Option[MaybeProxy[StructType]]): Unit

  def subComponents: immutable.Seq[ComponentRef]
}
