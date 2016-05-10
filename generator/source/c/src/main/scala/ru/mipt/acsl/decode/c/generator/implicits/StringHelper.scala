package ru.mipt.acsl.decode.c.generator.implicits

import com.google.common.base.CaseFormat
import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.generator.c.ast.implicits._
import ru.mipt.acsl.generator.c.ast.{CAstElements => _, _}

/**
  * @author Artem Shein
  */
private[generator] case class StringHelper(str: String) {

  def _var: CVar = CVar(str)

  def call(exprs: CExpression*) = CFuncCall(str, exprs: _*)

  def tryCall(exprs: CExpression*) = str.call(exprs: _*)._try

  def methodName(name: String): String = str + "_" + name.capitalize

  def initMethodName: String = methodName(typeInitMethodName)

  def comment: CAstElements = Seq(CEol, CComment(str), CEol)

  def upperCamel2UpperUnderscore: String = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, str)

  def lowerUnderscore2UpperCamel: String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str)

  def upperCamel2LowerCamel: String = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str)

}
