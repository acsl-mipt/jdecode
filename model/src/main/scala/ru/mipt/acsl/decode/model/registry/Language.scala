package ru.mipt.acsl.decode.model.registry

/**
  * @author Artem Shein
  */
trait Language {
  def code: String
}

object Language {

  private case class LanguageImpl(code: String) extends Language

  def apply(code: String): Language =
    new LanguageImpl(code)
}
