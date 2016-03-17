package ru.mipt.acsl.decode.model.domain.pure.naming

/**
  * Element's name
  */
trait ElementName {
  def asMangledString: String
}