package ru.mipt.acsl.generator.c.ast

/**
  * Created by metadeus on 04.04.16.
  */

private[generator] trait CCommented[T <: CAstElement] { self: T =>

  def element: T
  def prepend: Option[String]
  def append: Option[String]

  override def generate(s: CGenState): Unit = {
    prepend.foreach(p => CComment(p).generate(s))
    element.generate(s)
    append.foreach(a => CComment(a).generate(s))
  }

}

object CCommented {
  def apply(expr: CExpression, prepend: String = null, append: String = null): CCommentedExpression =
    CCommentedExpression(expr, Option(prepend), Option(append))
}
