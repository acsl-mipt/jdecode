package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.pure.Language

/**
  * @author Artem Shein
  */
object Language {
  def apply(code: String): Language =
    new LanguageImpl(code)
}