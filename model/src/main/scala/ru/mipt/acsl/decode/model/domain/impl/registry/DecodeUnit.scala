package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.impl.types.DecodeUnitImpl
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.registry.DecodeUnit

/**
  * @author Artem Shein
  */
object DecodeUnit {
  def apply(name: ElementName, namespace: Namespace, display: Option[String] = None,
            info: Option[String] = None): DecodeUnit =
    new DecodeUnitImpl(name, namespace, display, info)
}
