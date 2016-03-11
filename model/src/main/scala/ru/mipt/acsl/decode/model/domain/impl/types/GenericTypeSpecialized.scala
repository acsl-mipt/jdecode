package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, GenericType, GenericTypeSpecialized}

/**
  * @author Artem Shein
  */
object GenericTypeSpecialized {
  def apply(name: ElementName, namespace: Namespace, info: LocalizedString,
            genericType: MaybeProxy[GenericType],
            genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]]): GenericTypeSpecialized =
    new GenericTypeSpecializedImpl(name, namespace, info, genericType, genericTypeArguments)
}
