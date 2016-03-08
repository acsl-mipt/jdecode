package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType

import scala.collection.immutable

trait Command extends HasOptionInfo with HasName with HasOptionId {
  def returnType: Option[MaybeProxy[DecodeType]]

  def parameters: immutable.Seq[Parameter]
}