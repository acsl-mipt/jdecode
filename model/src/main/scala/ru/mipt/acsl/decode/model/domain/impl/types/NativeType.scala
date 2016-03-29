package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
trait NativeType extends pure.types.NativeType with DecodeType {
  override def toString: String = "NativeType" + super.toString
}

object NativeType {
  def apply(name: ElementName, ns: Namespace, info: LocalizedString): NativeType = new NativeTypeImpl(name, ns, info)
}
