package ru.mipt.acsl.decode.model.domain.naming

/**
  * Fully qualified name
  * Identifies the namespace
  */
trait Fqn {
  def parts: Seq[ElementName]

  def asMangledString: String = parts.map(_.asMangledString).mkString(".")

  def last: ElementName = parts.last

  def copyDropLast: Fqn

  def size: Int = parts.size

  def isEmpty: Boolean = parts.isEmpty
}