package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.HasInfo
import ru.mipt.acsl.decode.model.domain.expr.ConstExpr
import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * Created by metadeus on 08.03.16.
  */
trait EnumConstant extends HasInfo {
  def name: ElementName

  def value: ConstExpr
}
