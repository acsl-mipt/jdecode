package ru.mipt.acsl.decode.model.domain.pure.component

/**
  * @author Artem Shein
  */
trait ComponentRef {
  def component: Component

  def alias: Option[String]
}
