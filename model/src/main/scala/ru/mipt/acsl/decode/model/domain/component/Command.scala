package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.types.Parameter
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, HasName}
import ru.mipt.acsl.decode.model.domain.types.DecodeType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Command extends HasInfo with HasName with HasOptionId {

  def parameters: immutable.Seq[Parameter]

  def returnTypeProxy: Option[MaybeProxy[DecodeType]]

  def returnType: Option[DecodeType] = returnTypeProxy.map(_.obj)

}

object Command {
  def apply(name: ElementName, id: Option[Int], info: LocalizedString,
            parameters: immutable.Seq[Parameter], returnTypeProxy: Option[MaybeProxy[DecodeType]]): Command =
    new CommandImpl(name, id, info, parameters, returnTypeProxy)
}
