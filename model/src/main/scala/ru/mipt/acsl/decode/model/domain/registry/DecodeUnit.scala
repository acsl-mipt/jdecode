package ru.mipt.acsl.decode.model.domain.registry

import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.{HasInfo, NamespaceAware, Referenceable, Validatable}
import ru.mipt.acsl.decode.model.domain.naming.HasName

/**
  * Created by metadeus on 08.03.16.
  */
trait DecodeUnit extends HasName with HasInfo with Referenceable with NamespaceAware with Validatable {
  def display: LocalizedString
}
