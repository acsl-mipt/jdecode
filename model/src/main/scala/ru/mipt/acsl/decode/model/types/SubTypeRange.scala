package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.expr.ConstExpr

/**
  * @author Artem Shein
  */
trait SubTypeRange {
  def from: Option[ConstExpr]
  def to: Option[ConstExpr]
}
