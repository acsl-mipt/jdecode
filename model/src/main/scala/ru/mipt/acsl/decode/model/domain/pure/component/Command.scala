package ru.mipt.acsl.decode.model.domain.pure.component

import ru.mipt.acsl.decode.model.domain.{HasInfo, HasOptionId}
import ru.mipt.acsl.decode.model.domain.pure.Parameter
import ru.mipt.acsl.decode.model.domain.pure.naming.HasName
import ru.mipt.acsl.decode.model.domain.pure.types.DecodeType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Command extends HasInfo with HasName with HasOptionId {
  def returnType: Option[DecodeType]

  def parameters: immutable.Seq[Parameter]
}
