package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName, Namespace}

/**
  * @author Artem Shein
  */
trait AliasType extends HasBaseType with DecodeType with HasName

object AliasType {

  private class Impl(name: ElementName, namespace: Namespace, info: LocalizedString,
                     typeUnit: TypeMeasure)
    extends AbstractBaseTypedType(name, namespace, info, typeUnit.typeProxy) with AliasType

  def apply(name: ElementName, namespace: Namespace, typeUnit: TypeMeasure,
            info: LocalizedString): AliasType = new Impl(name, namespace, info, typeUnit)
}
