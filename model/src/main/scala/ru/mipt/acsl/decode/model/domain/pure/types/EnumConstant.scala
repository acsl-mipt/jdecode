package ru.mipt.acsl.decode.model.domain.pure.types

import ru.mipt.acsl.decode.model.domain.pure.HasInfo
import ru.mipt.acsl.decode.model.domain.pure.expr.ConstExpr
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * Created by metadeus on 08.03.16.
  */
trait EnumConstant extends HasInfo {
  def name: ElementName

  def value: ConstExpr
}