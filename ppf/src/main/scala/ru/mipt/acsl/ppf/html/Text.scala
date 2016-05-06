package ru.mipt.acsl.ppf.html

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
case class Text(s: String) extends HtmlInfo {
  override def toHtml: Seq[Modifier] = Seq(s)
}
