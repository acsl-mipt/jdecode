package ru.mipt.acsl.decode.model.domain.impl

import org.parboiled2._
import shapeless.HList

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
  /*Rule Parameter()
    {
        Var<Buffer<Either<String, Int>>> tokens = new Var<>(new ArrayBuffer<>());
        return Sequence(ElementName(tokens), ZeroOrMore(FirstOf(Sequence('.', ElementName(tokens)), ArrayLimits(
                tokens))), EOI, push(tokens.get()));
    }

    Rule ElementName(@NotNull Var<Buffer<Either<String, Int>>> tokens)
    {
        return Sequence(Sequence(Optional('^'), FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), '_'), ZeroOrMore(
                FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), CharRange('0', '9'), '_'))),
                tokens.get().$plus$eq(Either.left(DecodeNameImpl.newFromSourceName(match()).asString())));
    }

    Rule ArrayLimits(@NotNull Var<Buffer<Either<String, Int>>> tokens)
    {
        Var<Integer> lengthVar = new Var<>();
        return Sequence('[', OneOrMore(CharRange('0', '9')), lengthVar.set(Integer.parseInt(match())), ']',
                tokens.get().$plus$eq(Either.right((Int) (Object) lengthVar.get())));
    }*/
