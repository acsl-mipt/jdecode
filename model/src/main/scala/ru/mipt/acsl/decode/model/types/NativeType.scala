package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}

/**
  * @author Artem Shein
  */
trait NativeType extends GenericType {
  override def toString: String = "NativeType" + super.toString
}

object NativeType {

  private class Impl(name: ElementName, ns: Namespace, info: LocalizedString, val typeParameters: Seq[ElementName])
    extends AbstractType(name, ns, info) with NativeType

  def apply(name: ElementName, ns: Namespace, info: LocalizedString, typeParameters: Seq[ElementName]): NativeType =
    new Impl(name, ns, info, typeParameters)
}
