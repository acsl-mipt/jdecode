package ru.mipt.acsl.decode.model.domain.pure.types

import ru.mipt.acsl.decode.model.domain.pure.expr.ConstExpr

/**
  * @author Artem Shein
  */
trait SubTypeRange {
  def from: Option[ConstExpr]
  def to: Option[ConstExpr]
}
