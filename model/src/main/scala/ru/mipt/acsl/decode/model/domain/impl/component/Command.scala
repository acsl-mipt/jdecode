package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.types.{DecodeType, Parameter}
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Command extends pure.component.Command {
  def returnTypeProxy: Option[MaybeProxy[DecodeType]]
  override def returnType: Option[DecodeType] = returnTypeProxy.map(_.obj)
  override def parameters: immutable.Seq[Parameter]
}

object Command {
  def apply(name: ElementName, id: Option[Int], info: LocalizedString,
            parameters: immutable.Seq[Parameter], returnTypeProxy: Option[MaybeProxy[DecodeType]]): Command =
    new CommandImpl(name, id, info, parameters, returnTypeProxy)
}
