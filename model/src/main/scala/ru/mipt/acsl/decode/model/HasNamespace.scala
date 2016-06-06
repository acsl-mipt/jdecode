package ru.mipt.acsl.decode.model

import ru.mipt.acsl.decode.model.naming.Namespace

/**
  * @author Artem Shein
  */
trait HasNamespace {
  def namespace: Namespace
}
