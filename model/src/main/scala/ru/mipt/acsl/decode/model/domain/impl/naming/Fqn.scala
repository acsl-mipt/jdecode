package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.impl.ElementName
import ru.mipt.acsl.decode.model.domain.impl.types.FqnImpl
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn}

/**
  * @author Artem Shein
  */
object Fqn {
  def apply(parts: Seq[ElementName]): Fqn = FqnImpl(parts)
  def newFromFqn(fqn: Fqn, last: ElementName): Fqn = FqnImpl(fqn.parts :+ last)
  def newFromSource(sourceText: String): Fqn =
    FqnImpl("\\.".r.split(sourceText).map(ElementName.newFromSourceName))
}
