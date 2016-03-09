package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.aliases.ElementInfo
import ru.mipt.acsl.decode.model.domain.impl.ElementInfo
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit

/**
  * @author Artem Shein
  */
object DecodeUnit {
  def apply(name: ElementName, namespace: Namespace, display: Option[String] = None,
            info: ElementInfo = ElementInfo.empty): DecodeUnit =
    new DecodeUnitImpl(name, namespace, display, info)
}
