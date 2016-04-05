package ru.mipt.acsl.generator.c.ast

/**
  * Created by metadeus on 04.04.16.
  */
case class CULongLiteral(value: Long) extends CExpression {
  require(value >= 0)
  override def generate(s: CGenState): Unit = s.append(value.toString)
}
