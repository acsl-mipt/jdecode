package ru.mipt.acsl.decode.model.expr

import ru.mipt.acsl.decode.model.{Referenceable, ReferenceableVisitor}

/**
  * @author Artem Shein
  */
trait ConstExpr extends Referenceable {

  def toLongOrFail: Long = this match {
    case d: BigDecimalLiteral => d.value.toLongExact
    case _ =>
      sys.error("not a long value")
  }

  def accept(visitor: ReferenceableVisitor) {
    visitor.visit(this)
  }

}
