package ru.mipt.acsl.decode.model.component

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.{AbstractNameInfoAware, DecodeType, Parameter}

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

  private class CommandImpl(name: ElementName, val id: Option[Int], info: LocalizedString,
                            val parameters: immutable.Seq[Parameter], val returnTypeProxy: Option[MaybeProxy[DecodeType]])
    extends AbstractNameInfoAware(name, info) with Command

  def apply(name: ElementName, id: Option[Int], info: LocalizedString,
            parameters: immutable.Seq[Parameter], returnTypeProxy: Option[MaybeProxy[DecodeType]]): Command =
    new CommandImpl(name, id, info, parameters, returnTypeProxy)
}
