package ru.mipt.acsl.decode.model

import ru.mipt.acsl.decode.model.component.Command
import ru.mipt.acsl.decode.model.component.message.TmMessage

/**
  * @author Artem Shein
  */
trait CommandOrTmMessage extends Referenceable {

  def toEither: Either[Command, TmMessage]

}

object CommandOrTmMessage {

  private final case class CommandOrTmMessageImpl(toEither: Either[Command, TmMessage]) extends CommandOrTmMessage

  def apply(either: Either[Command, TmMessage]): CommandOrTmMessage = CommandOrTmMessageImpl(either)

}
