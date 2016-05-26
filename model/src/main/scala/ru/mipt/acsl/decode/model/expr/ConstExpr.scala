package ru.mipt.acsl.decode.model.expr

/**
  * @author Artem Shein
  */
trait ConstExpr {
  def toLongOrFail: Long = this match {
    case d: BigDecimalLiteral => d.value.toLongExact
    case _ =>
      sys.error("not a long value")
  }
}
