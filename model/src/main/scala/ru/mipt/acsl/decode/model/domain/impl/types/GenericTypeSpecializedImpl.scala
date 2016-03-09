package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, GenericType, GenericTypeSpecialized}

/**
  * @author Artem Shein
  */
private class GenericTypeSpecializedImpl(name: ElementName, namespace: Namespace, info: ElementInfo,
                                         val genericType: MaybeProxy[GenericType],
                                         val genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]])
  extends AbstractType(name, namespace, info) with GenericTypeSpecialized
