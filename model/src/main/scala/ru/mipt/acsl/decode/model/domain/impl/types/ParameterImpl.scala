package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
// Components
private class ParameterImpl(name: ElementName, info: Option[String], val paramType: MaybeProxy[DecodeType],
                            val unit: Option[MaybeProxy[DecodeUnit]])
  extends AbstractNameAndOptionalInfoAware(name, info) with Parameter
