package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Fqn
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
private abstract class AbstractType(name: ElementName, var namespace: Namespace, info: LocalizedString)
  extends AbstractHasNameAndInfo(name, info) with DecodeType {
  def fqn: Fqn = Fqn(namespace.fqn.parts :+ name)
}
