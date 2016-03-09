package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.component.{Command, Parameter}
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractOptionalInfoAware
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
private class CommandImpl(val name: ElementName, val id: Option[Int], info: ElementInfo,
                          val parameters: immutable.Seq[Parameter], val returnType: Option[MaybeProxy[DecodeType]])
  extends AbstractOptionalInfoAware(info) with Command
