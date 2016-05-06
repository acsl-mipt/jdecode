package ru.mipt.acsl.ppf.html

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
case class Html(els: Modifier*) extends HtmlInfo {
  override def toHtml: Seq[Modifier] = els
}
