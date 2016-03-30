package ru.mipt.acsl.decode.model.domain.pure.component.messages

/**
  * Created by metadeus on 30.03.16.
  */
trait ElementRange {
  def lowerBound: Long
  def upperBound: Option[Long]
}
