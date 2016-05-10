package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace

/**
  * @author Artem Shein
  */
trait NamespaceAware {
  def namespace: Namespace
}
