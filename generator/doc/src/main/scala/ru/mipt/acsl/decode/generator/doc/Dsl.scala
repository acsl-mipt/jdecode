package ru.mipt.acsl.decode.generator.doc

import scala.collection.mutable

/**
  * @author Artem Shein
  */
object Dsl {

  trait TextElement

  class RawText(val text: String) extends TextElement

  class Para(val contents: mutable.Buffer[TextElement] = mutable.Buffer.empty)

  class Section(val title: String, var doc: Doc, val contents: mutable.Buffer[Para] = mutable.Buffer.empty) {
    def apply(append: Para*): Section = {
      contents ++= append
      this
    }
  }

  class Doc(val title: String, val sections: mutable.Buffer[Section] = mutable.Buffer.empty) {
    def section(sectionTitle: String) = {
      val section = new Section(sectionTitle, this)
      this.sections += section
      section
    }
  }

  class Li(val contents: Seq[TextElement] = Seq.empty)

  class Ul(val lis: Seq[Li] = Seq.empty) extends TextElement

  implicit def string2RawText(str: String): RawText = new RawText(str)

  def p(textElements: TextElement*): Para = new Para(textElements.toBuffer)

  def ul(lis: Li*): Ul = new Ul(lis)

  def li(textElements: TextElement*): Li = new Li(textElements)

  def document(title: String): Doc = new Doc(title)
}
