package ru.mipt.acsl.decode.model.naming

/**
  * Fully qualified name
  * Identifies namespace or any other entity
  */
trait Fqn {
  def parts: Seq[ElementName]

  def asMangledString: String = parts.map(_.asMangledString).mkString(".")

  def last: ElementName = parts.last

  def copyDropLast: Fqn

  def size: Int = parts.size

  def isEmpty: Boolean = parts.isEmpty
}

object Fqn {

  private case class Impl(parts: Seq[ElementName]) extends Fqn {
    def copyDropLast: Fqn = Impl(parts.dropRight(1))
  }

  val SystemNamespace = Fqn.newFromSource("decode")
  def apply(parts: Seq[ElementName]): Fqn = Impl(parts)
  def newFromFqn(fqn: Fqn, last: ElementName): Fqn = Impl(fqn.parts :+ last)
  def newFromSource(sourceText: String): Fqn =
    Impl("\\.".r.split(sourceText).map(ElementName.newFromSourceName))
}
