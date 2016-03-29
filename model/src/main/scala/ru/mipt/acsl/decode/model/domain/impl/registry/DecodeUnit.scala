package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
trait DecodeUnit extends pure.registry.DecodeUnit {
  def namespace_=(ns: Namespace): Unit
}

object DecodeUnit {
  def apply(name: ElementName, namespace: Namespace, display: LocalizedString = LocalizedString.empty,
            info: LocalizedString = LocalizedString.empty): DecodeUnit =
    new DecodeUnitImpl(name, namespace, display, info)
}
