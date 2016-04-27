package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.pure.{naming => n}

/**
  * @author Artem Shein
  */
private case class FqnImpl(parts: Seq[n.ElementName]) extends n.Fqn {
  def copyDropLast: n.Fqn = FqnImpl(parts.dropRight(1))
}
