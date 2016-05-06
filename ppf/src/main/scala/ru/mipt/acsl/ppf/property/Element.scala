package ru.mipt.acsl.ppf.property

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, HtmlInfo}
import ru.mipt.acsl.ppf.types.BaseType

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
class Element(val title: String, val t: Option[BaseType], val value: HtmlInfo = EmptyHtmlInfo, val info: HtmlInfo = EmptyHtmlInfo) {
  def toHtml: Seq[Modifier] = Seq(tr(
    td(title),
    td(t.map[String](_.toString).getOrElse[String]("")),
    td(value.toHtml: _*),
    td(info.toHtml: _*)))
}

object Element {

  def apply(title: String, t: Option[BaseType], value: HtmlInfo, info: HtmlInfo): Element =
    new Element(title, t, value, info)

  def apply(title: String, t: Option[BaseType], value: HtmlInfo): Element =
    apply(title, t, value, EmptyHtmlInfo)

  def apply(title: String, t: Option[BaseType]): Element = apply(title, t, EmptyHtmlInfo)

  def apply(title: String): Element = apply(title, None)

  def apply(title: String, t: BaseType, value: HtmlInfo = EmptyHtmlInfo, info: HtmlInfo = EmptyHtmlInfo): Element =
    apply(title, Some(t), value, info)

}
