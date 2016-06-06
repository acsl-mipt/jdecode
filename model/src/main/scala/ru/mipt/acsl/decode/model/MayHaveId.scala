package ru.mipt.acsl.decode.model

/**
  * @author Artem Shein
  */
trait MayHaveId {

  def id: Option[Int]

}
