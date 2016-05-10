package ru.mipt.acsl.decode.model.domain.registry

import ru.mipt.acsl.decode.model.domain.Referenceable
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Registry extends Referenceable {
  def rootNamespaces: immutable.Seq[Namespace]
}
