package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
trait NativeType extends DecodeType {
  override def toString: String = "NativeType" + super.toString
}

object NativeType {
  def apply(name: ElementName, ns: Namespace, info: LocalizedString): NativeType = new NativeTypeImpl(name, ns, info)
}
