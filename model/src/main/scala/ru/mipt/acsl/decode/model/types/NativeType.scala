package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.Namespace
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.types.DecodeType

/**
  * @author Artem Shein
  */
trait NativeType extends DecodeType {
  override def toString: String = "NativeType" + super.toString
}

object NativeType {

  private class Impl(name: ElementName, ns: Namespace, info: LocalizedString)
    extends AbstractType(name, ns, info) with NativeType

  def apply(name: ElementName, ns: Namespace, info: LocalizedString): NativeType =
    new Impl(name, ns, info)
}
