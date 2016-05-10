package ru.mipt.acsl.decode.model.domain.pure.registry

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure.Referenceable

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Registry extends Referenceable {
  def rootNamespaces: immutable.Seq[Namespace]
}
