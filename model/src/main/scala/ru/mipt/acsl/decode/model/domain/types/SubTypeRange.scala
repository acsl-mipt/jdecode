package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.expr.ConstExpr

/**
  * @author Artem Shein
  */
trait SubTypeRange {
  def from: Option[ConstExpr]
  def to: Option[ConstExpr]
}
