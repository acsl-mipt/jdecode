package ru.mipt.acsl.ppf.property

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Html, Text}
import ru.mipt.acsl.ppf.packet.Packet
import ru.mipt.acsl.ppf.types.Varuint

import scalatags.Text.all.{Tag => _, _}

/**
  * @author Artem Shein
  */
class Address(val kind: Option[Int], val beforeElementsOverride: Option[Seq[Element]] = None) extends Property {

  val SourceAddressElement = Element("Адрес источника", Varuint)
  val DestinationAddressElement = Element("Адрес приемника", Varuint)

  kind.foreach { k =>
    require(beforeElementsOverride.forall(_.size == k + 1))
  }

  val rank = 5

  def beforeElements(packet: Packet) = beforeElementsOverride match {
    case Some(els) => els
    case None =>
      kind match {
        case Some(k) => Element("Вид адресации", Varuint, Text(k.toString)) +: (k match {
          case 2 => Seq(SourceAddressElement, DestinationAddressElement)
          case _ => sys.error("not implemented")
        })
        case None => Seq(Element("Вид адресации", Varuint), Element("Абоненты (адреса)", None, EmptyHtmlInfo,
          Html(table(cls := "table table-bordered", thead(tr(th("Вид адресации"), th("Название"), th("Тип"))),
            tbody(
              tr(td(rowspan := 2, p(textAlign := "center", 2.toString)), td("Адрес источника"), td(Varuint.toString)),
              tr(td("Адрес приемника"), td(Varuint.toString)),
              tr(td(rowspan := 4, p(textAlign := "center", 4.toString)), td("Адрес источника"), td(Varuint.toString)),
              tr(td("Адрес приемника"), td(Varuint.toString)),
              tr(td("Порт/компонент источника"), td(Varuint.toString)),
              tr(td("Порт/компонент приемника"), td(Varuint.toString)),
              tr(td(rowspan := 6, p(textAlign := "center", 6.toString)), td("Адрес источника"), td(Varuint.toString)),
              tr(td("Адрес приемника"), td(Varuint.toString)),
              tr(td("Порт/компонент источника"), td(Varuint.toString)),
              tr(td("Порт/компонент приемника"), td(Varuint.toString)),
              tr(td("Группа источника"), td(Varuint.toString)),
              tr(td("Группа приемника"), td(Varuint.toString)))))))
      }
  }

}

object Address {

  def apply(): Address = apply(None)

  def apply(kind: Option[Int]): Address = new Address(kind)

  def apply(kind: Int, beforeElementsOverride: Seq[Element]): Address = new Address(Some(kind), Some(beforeElementsOverride))

}
