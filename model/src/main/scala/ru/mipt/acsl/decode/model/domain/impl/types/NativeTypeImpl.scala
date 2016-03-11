package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.types.NativeType

/**
  * @author Artem Shein
  */
private class NativeTypeImpl(name: ElementName, ns: Namespace, info: LocalizedString)
  extends AbstractType(name, ns, info) with NativeType
