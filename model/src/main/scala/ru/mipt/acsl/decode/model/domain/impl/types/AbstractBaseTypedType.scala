package ru.mipt.acsl.decode.model.domain
package impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
abstract class AbstractBaseTypedType(name: ElementName, namespace: Namespace, info: LocalizedString,
                                     val baseTypeProxy: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info) with HasBaseType
