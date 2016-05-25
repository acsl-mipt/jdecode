package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.Namespace
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait SubType extends GenericType with HasBaseType {

  def typeMeasure: TypeMeasure

  def baseTypeProxy: MaybeProxy[DecodeType] = typeMeasure.typeProxy

  override def baseType: DecodeType = baseTypeProxy.obj
}

object SubType {

  private class Impl(name: ElementName, namespace: Namespace, info: LocalizedString,
                     val typeMeasure: TypeMeasure, val typeParameters: Seq[ElementName])
    extends AbstractType(name, namespace, info) with SubType

  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            typeMeasure: TypeMeasure, typeParameters: Seq[ElementName]): SubType =
    new Impl(name, namespace, info, typeMeasure, typeParameters)
}
