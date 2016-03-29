package ru.mipt.acsl.decode.model.domain.pure.registry

import ru.mipt.acsl.decode.model.domain.pure.{HasInfo, LocalizedString, NamespaceAware, Referenceable}

/**
  * @author Artem Shein
  */
trait DecodeUnit extends HasInfo with Referenceable with NamespaceAware {
  def display: LocalizedString
}
