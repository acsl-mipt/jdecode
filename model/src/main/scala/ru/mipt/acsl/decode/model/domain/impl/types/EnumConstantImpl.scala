package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.expr.ConstExpr
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.EnumConstant

/**
  * @author Artem Shein
  */
private class EnumConstantImpl(val name: ElementName, val value: ConstExpr, info: LocalizedString)
  extends AbstractHasInfo(info) with EnumConstant
