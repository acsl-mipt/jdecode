package ru.mipt.acsl.decode.model.domain.pure.registry

import ru.mipt.acsl.decode.model.domain.pure.Referenceable
import ru.mipt.acsl.decode.model.domain.pure.naming.Namespace

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Registry extends Referenceable {
  def rootNamespaces: immutable.Seq[Namespace]
}
