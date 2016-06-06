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

  private case class FqnImpl(parts: Seq[ElementName]) extends Fqn {
    def copyDropLast: Fqn = FqnImpl(parts.dropRight(1))
  }

  val DecodeNamespace = Fqn("decode")

  val Or = Fqn(DecodeNamespace, ElementName.newFromMangledName("or"))

  val Option = Fqn(DecodeNamespace, ElementName.newFromMangledName("option"))

  val Unit = Fqn(DecodeNamespace, ElementName.newFromMangledName("unit"))

  val Array = Fqn(DecodeNamespace, ElementName.newFromMangledName("array"))

  val Varuint = Fqn(Fqn.DecodeNamespace, ElementName.newFromMangledName("varuint"))

  val Range = Fqn(Fqn.DecodeNamespace, ElementName.newFromMangledName("range"))

  def apply(parts: Seq[ElementName]): Fqn = FqnImpl(parts)

  def apply(fqn: Fqn, last: ElementName): Fqn = FqnImpl(fqn.parts :+ last)

  def apply(sourceText: String): Fqn =
    FqnImpl("\\.".r.split(sourceText).map(ElementName.newFromSourceName))
}
