package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private class AliasTypeImpl(name: ElementName, namespace: Namespace, info: LocalizedString,
                            baseTypeProxy: MaybeProxy[DecodeType])
  extends AbstractBaseTypedType(name, namespace, info, baseTypeProxy) with AliasType
