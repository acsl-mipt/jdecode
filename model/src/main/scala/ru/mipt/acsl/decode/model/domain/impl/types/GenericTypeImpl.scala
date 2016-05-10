package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private class GenericTypeImpl(name: ElementName, ns: Namespace, info: LocalizedString,
                              val typeParameters: Seq[Option[ElementName]])
  extends AbstractType(name, ns, info) with GenericType
