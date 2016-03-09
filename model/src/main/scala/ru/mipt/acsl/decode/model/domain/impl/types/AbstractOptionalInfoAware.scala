package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private[domain] abstract class AbstractOptionalInfoAware(val info: Option[String]) extends HasOptionInfo
