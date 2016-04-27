package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.{pure => p}

/**
  * @author Artem Shein
  */
object Language {
  def apply(code: String): p.Language =
    new LanguageImpl(code)
}
