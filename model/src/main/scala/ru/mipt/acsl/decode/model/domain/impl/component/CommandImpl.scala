package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.impl.types.{AbstractNameInfoAware, DecodeType, Parameter}
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString

import scala.collection.immutable

/**
  * @author Artem Shein
  */
private class CommandImpl(name: ElementName, val id: Option[Int], info: LocalizedString,
                          val parameters: immutable.Seq[Parameter], val returnTypeProxy: Option[MaybeProxy[DecodeType]])
  extends AbstractNameInfoAware(name, info) with Command
