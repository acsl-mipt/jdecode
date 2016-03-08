package ru.mipt.acsl.decode.model.domain.registry

import ru.mipt.acsl.decode.model.domain.{HasOptionInfo, NamespaceAware, Referenceable, Validatable}
import ru.mipt.acsl.decode.model.domain.naming.HasName

/**
  * Created by metadeus on 08.03.16.
  */
trait DecodeUnit extends HasName with HasOptionInfo with Referenceable with NamespaceAware with Validatable {
  def display: Option[String]
}
