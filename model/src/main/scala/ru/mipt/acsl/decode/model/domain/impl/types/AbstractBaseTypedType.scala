package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
abstract class AbstractBaseTypedType(name: ElementName, namespace: Namespace, info: LocalizedString,
                                     val baseTypeProxy: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info) with BaseTypedType
