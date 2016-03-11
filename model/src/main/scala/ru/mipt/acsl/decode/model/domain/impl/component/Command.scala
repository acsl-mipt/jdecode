package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.component.{Command, Parameter}
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
object Command {
  def apply(name: ElementName, id: Option[Int], info: LocalizedString,
            parameters: immutable.Seq[Parameter], returnType: Option[MaybeProxy[DecodeType]]): Command =
    new CommandImpl(name, id, info, parameters, returnType)
}
