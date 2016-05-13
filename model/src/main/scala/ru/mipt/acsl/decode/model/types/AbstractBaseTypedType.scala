package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
abstract class AbstractBaseTypedType(name: ElementName, namespace: Namespace, info: LocalizedString,
                                     val baseTypeProxy: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info) with HasBaseType
