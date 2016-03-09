package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.DecodeType

/**
  * @author Artem Shein
  */
private abstract class AbstractTypeWithBaseType(name: ElementName, namespace: Namespace,
                                                info: ElementInfo, var baseType: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info)
