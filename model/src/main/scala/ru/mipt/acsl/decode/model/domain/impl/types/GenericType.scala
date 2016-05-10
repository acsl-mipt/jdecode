package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
trait GenericType extends DecodeType {

  def typeParameters: Seq[Option[ElementName]]

  override def toString: String = "GenericType" + super.toString

}

object GenericType {
  def apply(name: ElementName, ns: Namespace, info: LocalizedString,
            typeParameters: Seq[Option[ElementName]]): GenericType =
    new GenericTypeImpl(name, ns, info, typeParameters)
}
