package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.types.GenericType

/**
  * @author Artem Shein
  */
private class GenericTypeImpl(name: ElementName, ns: Namespace, info: ElementInfo,
                              val typeParameters: Seq[Option[ElementName]])
  extends AbstractType(name, ns, info) with GenericType
