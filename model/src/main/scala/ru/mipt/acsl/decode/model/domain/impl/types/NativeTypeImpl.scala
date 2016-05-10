package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private class NativeTypeImpl(name: ElementName, ns: Namespace, info: LocalizedString)
  extends AbstractType(name, ns, info) with NativeType
