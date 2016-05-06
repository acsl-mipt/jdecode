package ru.mipt.acsl.ppf.html

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
trait HtmlInfo {
  def toHtml: Seq[Modifier]
}
