package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn}

/**
  * @author Artem Shein
  */
private case class FqnImpl(parts: Seq[ElementName]) extends Fqn {
  def copyDropLast: Fqn = FqnImpl(parts.dropRight(1))
}
