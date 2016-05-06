package ru.mipt.acsl.ppf.property

import ru.mipt.acsl.ppf.html.{EmptyHtmlInfo, Text}
import ru.mipt.acsl.ppf.packet.Packet
import ru.mipt.acsl.ppf.types.{U16, Varuint}

import scalatags.Text.all._

/**
  * @author Artem Shein
  */
class ErrorCorrectionCode(val kind: Option[Int] = None) extends Property {

  val rank = 6

  override def beforeElements(packet: Packet) = Seq(
    Element("Вид защиты данных пакета", Varuint, kind.map{ k => Text(k.toString) }.getOrElse(EmptyHtmlInfo)))

  override def afterElements(packet: Packet) = kind match {
    case Some(1) =>
      Seq(Element("Значение контрольной суммы", U16, Text(1.toString), Text("Значение CRC-16")))
    case None => Seq(Element("Значение контрольной суммы"))
    case _ => sys.error("not implemented")
  }

}

object ErrorCorrectionCode {

  def apply(kind: Int): ErrorCorrectionCode = new ErrorCorrectionCode(Some(kind))

  def apply(): ErrorCorrectionCode = new ErrorCorrectionCode(None)

}
