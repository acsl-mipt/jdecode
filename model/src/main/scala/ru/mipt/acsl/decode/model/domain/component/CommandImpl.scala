package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.types.{AbstractNameInfoAware, Parameter}
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.DecodeType

import scala.collection.immutable

/**
  * @author Artem Shein
  */
private class CommandImpl(name: ElementName, val id: Option[Int], info: LocalizedString,
                          val parameters: immutable.Seq[Parameter], val returnTypeProxy: Option[MaybeProxy[DecodeType]])
  extends AbstractNameInfoAware(name, info) with Command
