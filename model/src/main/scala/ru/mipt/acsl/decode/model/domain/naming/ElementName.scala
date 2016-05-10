package ru.mipt.acsl.decode.model.domain.naming

/**
  * Element's name
  */
trait ElementName {
  def asMangledString: String
}