package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private class GenericTypeSpecializedImpl(name: ElementName, namespace: Namespace, info: LocalizedString,
                                         val genericTypeProxy: MaybeProxy[GenericType],
                                         val genericTypeArgumentsProxy: Seq[Option[MaybeProxy[DecodeType]]])
  extends AbstractType(name, namespace, info) with GenericTypeSpecialized
