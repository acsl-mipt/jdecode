package ru.mipt.acsl.generator.html.ast

import ru.mipt.acsl.generation.Generatable

import scala.collection.immutable
import java.lang.{StringBuilder => JavaStringBuilder}

import com.google.common.html.HtmlEscapers

package object implicits {
  type HtmlAstElements = Seq[HtmlAstElement]
  type HtmlElements = Seq[HtmlElement]
  implicit class HtmlAstElementsGeneratable(els: HtmlAstElements) extends HtmlAstElement {
    override def generate(s: HtmlGenState): Unit = { els.foreach(_.generate(s)) }
  }
  implicit def str2html(str: String): HtmlString = HtmlString(str)
}

trait HtmlAstElement extends Generatable[HtmlGenState]

trait HtmlElement extends HtmlAstElement

class HtmlString(str: String) extends HtmlElement {
  private val contents: String = HtmlEscapers.htmlEscaper().escape(str)
  override def generate(s: HtmlGenState): Unit = s.append(contents)
  override def toString: String = contents
}

object HtmlString {
  def apply(str: String): HtmlString = new HtmlString(str)
}

case class HtmlUnsafeString(str: String) extends HtmlElement {
  override def generate(generatorState: HtmlGenState): Unit = generatorState.append(str)
  override def toString: String = str
}

case class Attr(name: String, value: String)

case class HtmlTag(tag: String, attrs: Seq[Attr] = immutable.Seq.empty,
                   contents: immutable.Seq[HtmlElement] = immutable.Seq.empty) extends HtmlElement {
  def generateAttrs(s: HtmlGenState) =
    s.append(attrs.map(attr => " " + attr.name + "=" + "\"" + attr.value + "\"").mkString)
  def apply(str: String): HtmlTag =
    new HtmlTag(tag, attrs, contents :+ HtmlString(str))
  def apply(_attr: Attr): HtmlTag =
    new HtmlTag(tag, attrs = attrs :+ _attr, contents)
  def apply(_contents: HtmlElement*): HtmlTag =
    new HtmlTag(tag, attrs, contents ++ _contents)
  // Possible optimization: ThreadLocal for StringBuilder, need to measure first
  override def toString: String = {
    val sb = new JavaStringBuilder()
    generate(HtmlGenState(sb))
    sb.toString
  }
  override def generate(s: HtmlGenState): Unit = {
    s.append(s"<$tag")
    generateAttrs(s)
    if (contents.isEmpty) {
      s.append("/>")
    } else {
      s.append(">")
      contents.foreach(_.generate(s))
      s.append(s"</$tag>")
    }
  }
}

case class HtmlTagExt(tag: String, attrs: Seq[Attr] = immutable.Seq.empty,
                   contents: immutable.Seq[HtmlElement] = immutable.Seq.empty) extends HtmlElement {
  def generateAttrs(s: HtmlGenState) =
    s.append(attrs.map(attr => " " + attr.name + "=" + "\"" + attr.value + "\"").mkString)
  def apply(str: String): HtmlTagExt =
    new HtmlTagExt(tag, attrs, contents :+ HtmlString(str))
  def apply(_attr: Attr): HtmlTagExt =
    new HtmlTagExt(tag, attrs = attrs :+ _attr, contents)
  def apply(_contents: HtmlElement*): HtmlTagExt =
    new HtmlTagExt(tag, attrs, contents ++ _contents)
  override def generate(s: HtmlGenState): Unit = {
    s.append(s"<$tag")
    generateAttrs(s)
    s.append(">")
    contents.foreach(_.generate(s))
    s.append(s"</$tag>")
  }
}

case class HtmlGenState(a: Appendable) {
  def append(s: String) = a.append(s)
}

object HttpEquivAttr {
  def apply(value: String): Attr = Attr("http-equiv", value)
}

object ContentAttr {
  def apply(value: String): Attr = Attr("content", value)
}

object CharsetAttr {
  def apply(value: String): Attr = Attr("charset", value)
}

object meta {
  def apply(httpEquiv: String = null, content: String = null, charset: String = null): HtmlTag =
    new HtmlTag("meta", Seq(Option(httpEquiv).map(HttpEquivAttr(_)), Option(content).map(ContentAttr(_)),
      Option(charset).map(CharsetAttr(_))).flatten)
}

case object doctype extends HtmlElement {
  override def generate(s: HtmlGenState): Unit = s.append("<!DOCTYPE html>")
}

package object tags {
  val html = new HtmlTag("html")
  val head = new HtmlTag("head")
  val title = new HtmlTag("title")
  val link = new HtmlTag("link")
  val script = new HtmlTagExt("script")
  val body = new HtmlTag("body")
  val div = new HtmlTag("div")
  val table = new HtmlTag("table")
  val tbody = new HtmlTag("tbody")
  val thead = new HtmlTag("thead")
  val tr = new HtmlTag("tr")
  val td = new HtmlTag("td")
  val th = new HtmlTag("th")
  val h1 = new HtmlTag("h1")
  val h2 = new HtmlTag("h2")
  val h3 = new HtmlTag("h3")
  val h4 = new HtmlTag("h4")
  val h5 = new HtmlTag("h5")
  val small = new HtmlTag("small")
  val br = new HtmlTag("br")
  val ul = new HtmlTag("ul")
  val ol = new HtmlTag("ol")
  val li = new HtmlTag("li")
  val a = new HtmlTag("a")
  val p = new HtmlTag("p")
  val hr = new HtmlTag("hr")
}

object rel {
  def apply(str: String): Attr = Attr("rel", str)
}

object id {
  def apply(str: String): Attr = Attr("id", str)
}

object href {
  def apply(str: String): Attr = Attr("href", str)
}

object src {
  def apply(str: String): Attr = Attr("src", str)
}

object _class {
  def apply(str: String): Attr = Attr("class", str)
}

object style {
  def apply(str: String): Attr = Attr("style", str)
}

object text {
  def apply(str: String): HtmlString = HtmlString(str)
}

object unsafe {
  def apply(str: String): HtmlUnsafeString = HtmlUnsafeString(str)
}