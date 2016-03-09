package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private[domain] class AbstractNameAndOptionalInfoAware(val name: ElementName, info: Option[String])
  extends AbstractOptionalInfoAware(info) with HasName
