package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.Namespace
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.types.DecodeType

/**
  * @author Artem Shein
  */
trait GenericType extends DecodeType {

  def typeParameters: Seq[Option[ElementName]]

  override def toString: String = "GenericType" + super.toString

}

object GenericType {

  private class Impl(name: ElementName, ns: Namespace, info: LocalizedString,
                                val typeParameters: Seq[Option[ElementName]])
    extends AbstractType(name, ns, info) with GenericType

  def apply(name: ElementName, ns: Namespace, info: LocalizedString,
            typeParameters: Seq[Option[ElementName]]): GenericType =
    new Impl(name, ns, info, typeParameters)
}
