package ru.mipt.acsl.decode.model.domain.impl.component.message

/**
  * @author Artem Shein
  */
private abstract class AbstractMessage(info: Option[String]) extends AbstractOptionalInfoAware(info) with TmMessage
