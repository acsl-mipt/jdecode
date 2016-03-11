package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit

/**
  * @author Artem Shein
  */
object DecodeUnit {
  def apply(name: ElementName, namespace: Namespace, display: LocalizedString = LocalizedString.empty,
            info: LocalizedString = LocalizedString.empty): DecodeUnit =
    new DecodeUnitImpl(name, namespace, display, info)
}
