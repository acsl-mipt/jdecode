package ru.mipt.acsl.ppf.html

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
case object EmptyHtmlInfo extends HtmlInfo {
  override def toHtml: Seq[Modifier] = Seq.empty
}
