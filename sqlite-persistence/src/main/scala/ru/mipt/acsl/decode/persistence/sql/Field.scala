package ru.mipt.acsl.decode.persistence.sql

/**
  * @author Artem Shein
  */
case class Field(name: String, t: SqlType) {
  override def toString: String = s"$name $t"
}
