package ru.mipt.acsl.decode.model.domain.pure

import ru.mipt.acsl.decode.model.domain.pure.naming.Namespace

/**
  * @author Artem Shein
  */
trait NamespaceAware {
  def namespace: Namespace
}
