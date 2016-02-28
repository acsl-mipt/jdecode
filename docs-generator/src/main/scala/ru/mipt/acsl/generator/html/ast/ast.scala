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