package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.expr.ConstExpr
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.EnumConstant

/**
  * @author Artem Shein
  */
private class EnumConstantImpl(val name: ElementName, val value: ConstExpr, info: LocalizedString)
  extends AbstractOptionalInfoAware(info) with EnumConstant
