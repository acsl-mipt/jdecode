package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName
import ru.mipt.acsl.decode.model.domain.pure.types.SubTypeRange

/**
  * @author Artem Shein
  */
private class SubTypeImpl(name: ElementName, namespace: Namespace, info: LocalizedString,
                          val baseTypeProxy: MaybeProxy[DecodeType], val range: Option[SubTypeRange])
  extends AbstractType(name, namespace, info) with SubType
