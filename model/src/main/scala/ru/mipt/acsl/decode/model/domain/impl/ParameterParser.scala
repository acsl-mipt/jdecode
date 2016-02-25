package ru.mipt.acsl.decode.model.domain.impl

import org.parboiled2._

/**
  * @author Artem Shein
  */
class ParameterParser(val input: ParserInput) extends Parser {
  def Parameter: Rule1[Seq[Either[String, Int]]] = rule {
    (Field ~> (Seq(_)) ~ zeroOrMore(FieldOrIndex)) ~> (_ ++ _) ~ EOI
  }

  def FieldOrIndex: Rule1[Either[String, Int]] = rule { '.' ~ Field | Index }

  def Field: Rule1[Left[String, Int]] = rule { ElementName ~> (Left[String, Int](_))}

  def Index: Rule1[Right[String, Int]] = rule { ArrayLimits ~> (Right[String, Int](_))}

  def ElementName: Rule1[String] = rule {
    capture(optional('^') ~ (CharPredicate.Alpha | '_') ~ zeroOrMore(CharPredicate.AlphaNum | '_'))
  }

  def ArrayLimits = rule {
    '[' ~ (capture(oneOrMore(CharPredicate.Digit)) ~> (_.toInt)) ~ ']'
  }
}
