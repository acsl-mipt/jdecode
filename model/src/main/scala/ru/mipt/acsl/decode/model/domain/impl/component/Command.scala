package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.impl.CommandImpl

/**
  * @author Artem Shein
  */
object Command {
  def apply(name: ElementName, id: Option[Int], info: Option[String],
            parameters: immutable.Seq[Parameter], returnType: Option[MaybeProxy[DecodeType]]): Command =
    new CommandImpl(name, id, info, parameters, returnType)
}
