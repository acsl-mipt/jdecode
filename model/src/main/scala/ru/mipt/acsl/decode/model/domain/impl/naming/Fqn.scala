package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.pure.{naming => n}

/**
  * @author Artem Shein
  */
object Fqn {
  val SystemNamespace = Fqn.newFromSource("decode")
  def apply(parts: Seq[n.ElementName]): n.Fqn = FqnImpl(parts)
  def newFromFqn(fqn: n.Fqn, last: n.ElementName): n.Fqn = FqnImpl(fqn.parts :+ last)
  def newFromSource(sourceText: String): n.Fqn =
    FqnImpl("\\.".r.split(sourceText).map(ElementName.newFromSourceName))
}
