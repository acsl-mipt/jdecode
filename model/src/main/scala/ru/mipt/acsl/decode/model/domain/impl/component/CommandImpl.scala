package ru.mipt.acsl.decode.model.domain.impl.component

/**
  * @author Artem Shein
  */
private class CommandImpl(val name: ElementName, val id: Option[Int], info: Option[String],
                          val parameters: immutable.Seq[Parameter], val returnType: Option[MaybeProxy[DecodeType]])
  extends AbstractOptionalInfoAware(info) with Command
