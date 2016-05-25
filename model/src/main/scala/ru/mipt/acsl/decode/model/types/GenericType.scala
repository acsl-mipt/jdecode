package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
trait GenericType extends DecodeType {

  def typeParameters: Seq[ElementName]

  override def toString: String = "GenericType" + super.toString

}
