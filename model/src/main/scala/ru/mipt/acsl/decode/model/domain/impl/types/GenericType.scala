package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.{LocalizedString, pure}
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
trait GenericType extends pure.types.GenericType with DecodeType {
  override def toString: String = "GenericType" + super.toString
}

object GenericType {
  def apply(name: ElementName, ns: Namespace, info: LocalizedString,
            typeParameters: Seq[Option[ElementName]]): GenericType =
    new GenericTypeImpl(name, ns, info, typeParameters)
}
