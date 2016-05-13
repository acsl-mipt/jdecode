package ru.mipt.acsl.decode.model.registry

/**
  * @author Artem Shein
  */
trait Language {
  def code: String
}

object Language {

  private case class Impl(code: String) extends Language

  def apply(code: String): Language =
    new Impl(code)
}
