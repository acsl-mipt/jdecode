package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.component.{Component, HasComponent}
import ru.mipt.acsl.decode.model.naming.{Container, HasName}
import ru.mipt.acsl.decode.model.types.Alias

/**
  * @author Artem Shein
  */
trait TmMessage extends MayHaveId with HasName with HasInfo with Container with HasComponent {

  def alias: Alias.ComponentTmMessage[_ <: TmMessage]

  override def accept[T](visitor: ContainerVisitor[T]): T = visitor.visit(this)

}