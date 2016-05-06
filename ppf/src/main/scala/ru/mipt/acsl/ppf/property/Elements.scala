package ru.mipt.acsl.ppf.property

import ru.mipt.acsl.ppf.html.HtmlInfo

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
case class Elements(els: Element*) extends HtmlInfo {
  override def toHtml: Seq[Modifier] = els.flatMap(_.toHtml)
}
