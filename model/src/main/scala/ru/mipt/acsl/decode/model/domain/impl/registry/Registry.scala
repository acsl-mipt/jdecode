package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.registry.Registry

/**
  * @author Artem Shein
  */
object Registry {
  def apply(): Registry = new RegistryImpl()
}
