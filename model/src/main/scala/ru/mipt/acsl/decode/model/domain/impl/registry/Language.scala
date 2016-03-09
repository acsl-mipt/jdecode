package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.impl.LanguageImpl

/**
  * @author Artem Shein
  */
object Language {
  def apply(name: ElementName, namespace: Namespace, isDefault: Boolean, info: Option[String]): Language =
    new LanguageImpl(name, namespace, isDefault, info)
}
