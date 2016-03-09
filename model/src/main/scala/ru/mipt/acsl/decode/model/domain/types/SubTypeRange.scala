package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.Validatable
import ru.mipt.acsl.decode.model.domain.expr.ConstExpr

/**
  * @author Artem Shein
  */
trait SubTypeRange extends Validatable {
  def from: Option[ConstExpr]
  def to: Option[ConstExpr]
}
