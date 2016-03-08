package ru.mipt.acsl.decode.model.domain.naming

import ru.mipt.acsl.decode.model.domain.NamespaceAware

/**
  * Created by metadeus on 08.03.16.
  */
trait Fqned extends HasName with NamespaceAware {
  def fqn: Fqn
}
