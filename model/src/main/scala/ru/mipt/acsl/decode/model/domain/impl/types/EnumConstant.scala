package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.expr.ConstExpr
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.EnumConstant

/**
  * @author Artem Shein
  */
object EnumConstant {
  def apply(name: ElementName, value: ConstExpr, info: LocalizedString): EnumConstant =
    new EnumConstantImpl(name, value, info)
}
