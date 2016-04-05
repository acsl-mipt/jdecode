package ru.mipt.acsl.generator.c.ast

/**
  * Created by metadeus on 04.04.16.
  */
// TODO: how to remove this stuff?
case class CCommentedExpression(element: CExpression, prepend: Option[String], append: Option[String])
  extends CCommented[CExpression] with CExpression
