package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class EnumConstantImpl(val name: ElementName, val value: ConstExpr, info: Option[String])
  extends AbstractOptionalInfoAware(info) with EnumConstant
