package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.impl.RegistryImpl

/**
  * @author Artem Shein
  */
object Registry {
  def apply(): Registry = new RegistryImpl()
}
